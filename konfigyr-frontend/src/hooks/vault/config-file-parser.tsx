import { useState } from 'react';
import { PropertyTransitionType } from '@konfigyr/hooks/vault/types';
import { parse } from 'yaml';
import type { ConfigurationProperty } from '@konfigyr/hooks/vault/types';

type Property = {
  name: string;
  value: string;
};

/**
 * Decodes escaped sequences in a string (commonly found in `.properties` files).
 *
 * @param value - The input string containing escape sequences.
 */
const decodeEscapes = (value: string) => {
  return value
    .replace(/\\u([0-9a-fA-F]{4})/g, (_, hex) => String.fromCharCode(parseInt(hex, 16)))
    .replace(/\\t/g, '\t')
    .replace(/\\r/g, '\r')
    .replace(/\\n/g, '\n')
    .replace(/\\f/g, '\f')
    .replace(/\\\\/g, '\\');
};

/**
 * Parses Java-style `.properties` file content into an array of `Property` objects.
 *
 * @param text - Raw `.properties` file content.
 */
const parseProperties = (text: string): Array<Property> => {
  const out: Array<Property> = [];

  const rawLines = text.replace(/\r\n?/g, '\n').split('\n');

  // Handle line continuations ending with an odd number of backslashes
  const lines: Array<string> = [];
  for (let i = 0; i < rawLines.length; i++) {
    let line = rawLines[i] ?? '';
    while (/\\$/.test(line) && !/\\\\$/.test(line)) {
      line = line.slice(0, -1) + (rawLines[++i] ?? '');
    }
    lines.push(line);
  }

  for (const original of lines) {
    const line = original.trim();
    if (!line || line.startsWith('#') || line.startsWith('!')) continue;

    // Find first unescaped separator: =, :, or whitespace
    let sep = -1;
    for (let i = 0; i < line.length; i++) {
      const ch = line[i];
      const prevBackslash = i > 0 && line[i - 1] === '\\';
      if (prevBackslash) continue;
      if (ch === '=' || ch === ':' || /\s/.test(ch)) {
        sep = i;
        break;
      }
    }

    let [key, value] = ['', ''];
    if (sep === -1) {
      [key, value] = [line, ''];
    } else {
      key = line.slice(0, sep).trimEnd();
      value = line.slice(sep);

      // If separator was whitespace, value may start immediately; if =/:, skip them
      value = value.replace(/^[\s:=]+/, '');
    }

    key = decodeEscapes(key.trim());
    value = decodeEscapes(value.trim());

    if (key) {
      out.push({ name: key, value });
    }
  }

  return out;
};

/**
 * Converts a JSON object into a flat string of Java-style `.properties` entries.
 *
 * @param obj - The input object to convert. Can contain nested objects and arrays.
 * @param prefix - A prefix used for nested keys. Used internally during recursion.
 */
function jsonToProperties (obj: any, prefix = ''): string {
  const lines: Array<string> = [];

  for (const key in obj) {
    const value = obj[key];
    const newKey = prefix ? `${prefix}.${key}` : key;

    if (Array.isArray(value)) {
      value.forEach((v, i) => {
        if (typeof v === 'object') {
          lines.push(jsonToProperties(v, `${newKey}[${i}]`));
        } else {
          lines.push(`${newKey}[${i}]=${v}`);
        }
      });
    } else if (typeof value === 'object' && value !== null) {
      lines.push(jsonToProperties(value, newKey));
    } else {
      lines.push(`${newKey}=${value}`);
    }
  }

  return lines.join('\n');
}

/**
 * Parses YAML text and converts it into an array of `Property` objects.
 *
 * @param yamlText - Raw YAML content to parse.
 */
const parseYaml = (yamlText?: string): Array<Property> => {
  if (!yamlText) {
    return [];
  }
  const parsedYamlObject = parse(yamlText);
  const propertiesString = jsonToProperties(parsedYamlObject);
  return parseProperties(propertiesString);
};

/**
 * Parses JSON text and converts it into an array of `Property` objects.
 *
 * @param text - Raw JSON string to parse.
 */
const parseJson = (text?: string): Array<Property> => {
  if (!text) {
    return [];
  }
  const parsedJsonObject = JSON.parse(text);
  const propertiesText = jsonToProperties(parsedJsonObject);
  return parseProperties(propertiesText);
};

/**
 * React hook for parsing configuration files into structured properties.
 *
 * Supports the following file formats:
 * - `.json`
 * - `.properties`
 * - `.yaml`
 * - `.yml`
 *
 * @returns An object containing:
 * - `properties` - The parsed configuration properties.
 * - `error` - An error message if parsing fails.
 * - `parse` - Async function to parse a given `File`.
 * - `reset` - Function to clear parsed data and errors.
 */
export function useConfigFileParser () {
  const [properties, setProperties] = useState<Array<ConfigurationProperty<any>>>([]);
  const [error, setError] = useState<string>();

  async function parseFile (file: File) {
    try {
      setError(undefined);

      const text = await file.text();
      const name = file.name.toLowerCase();

      let parsedProperties: Array<Property> = [];

      if (name.endsWith('.json')) {
        parsedProperties = parseJson(text);
      } else if (name.endsWith('.properties')) {
        parsedProperties = parseProperties(text);
      } else if (name.endsWith('.yaml') || name.endsWith('.yml')) {
        parsedProperties = parseYaml(text);
      } else {
        throw new Error('Unsupported file type');
      }

      const configurationProperties: Array<ConfigurationProperty<any>> = parsedProperties.map((p: Property) => {
        return {
          name: p.name,
          state: PropertyTransitionType.ADDED,
          value: {
            encoded: p.value,
            decoded: 'string',
          },
          typeName: 'string',
          schema: {
            type: 'string',
          },
        };
      });
      setProperties(configurationProperties);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Parse error');
      setProperties([]);
    }
  }

  const reset = () => {
    setProperties([]);
    setError(undefined);
  };

  return { properties, error, parseFile, reset };
}