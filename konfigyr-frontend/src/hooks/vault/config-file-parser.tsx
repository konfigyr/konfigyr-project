import { useRef, useState } from 'react';
import { PropertyTransitionType } from '@konfigyr/hooks/vault/types';
import { parse } from 'yaml';
import type { ConfigurationProperty } from '@konfigyr/hooks/vault/types';

type Property = {
  name: string;
  value: string;
};

const buildProperty = (name: string, value: string): Property => {
  return { name, value };
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
    // Java .properties allows escaping separators and special key chars.
    .replace(/\\([:=#!\s])/g, '$1')
    .replace(/\\\\/g, '\\');
};

const hasOddTrailingBackslashes = (value: string) => {
  let count = 0;
  for (let i = value.length - 1; i >= 0 && value[i] === '\\'; i--) {
    count++;
  }
  return count % 2 === 1;
};

const isEscaped = (value: string, index: number) => {
  let backslashes = 0;
  for (let i = index - 1; i >= 0 && value[i] === '\\'; i--) {
    backslashes++;
  }
  return backslashes % 2 === 1;
};

/**
 * Parses Java-style `.properties` file content into an array of `Property` objects.
 *
 * @param text - Raw `.properties` file content.
 */
const parsePropertiesFile = (text: string): Array<Property> => {
  const out: Array<Property> = [];

  const rawLines = text.replace(/\r\n?/g, '\n').split('\n');

  // Handle line continuations ending with an odd number of backslashes
  const lines: Array<string> = [];
  for (let i = 0; i < rawLines.length; i++) {
    let line = rawLines[i] ?? '';
    while (hasOddTrailingBackslashes(line)) {
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
      if (isEscaped(line, i)) continue;
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
      out.push(
        buildProperty(key, value),
      );
    }
  }

  return out;
};

/**
 * Checks whether a value is a scalar primitive.
 *
 * @param value - the value to evaluate.
 */
const isScalar = (value: unknown) => {
  return (
    value === null ||
    value === undefined ||
    typeof value === 'string' ||
    typeof value === 'number' ||
    typeof value === 'boolean' ||
    typeof value === 'bigint'
  );
};

/**
 * Converts a JSON-compatible object into flat `Property` entries.
 *
 * @param obj - The input object to convert. Can contain nested objects and arrays.
 */
function jsonToProperties (obj: unknown): Array<Property> {
  const properties: Array<Property> = [];

  const append = (value: unknown, prefix: string) => {
    if (!prefix) {
      return;
    }

    if (Array.isArray(value)) {
      if (value.every(isScalar)) {
        properties.push(
          buildProperty(prefix, value.map(v => String(v)).join(',')),
        );
        return;
      }

      value.forEach((item, index) => {
        append(item, `${prefix}[${index}]`);
      });
      return;
    }

    if (value !== null && typeof value === 'object') {
      Object.entries(value).forEach(([key, nestedValue]) => {
        const nestedKey = `${prefix}.${key}`;
        append(nestedValue, nestedKey);
      });
      return;
    }

    properties.push(
      buildProperty(prefix, String(value)),
    );
  };

  if (obj === null || obj === undefined || typeof obj !== 'object') {
    return [];
  }

  Object.entries(obj).forEach(([key, value]) => {
    append(value, key);
  });

  return properties;
}

/**
 * Parses YAML text and converts it into an array of `Property` objects.
 *
 * @param yamlText - Raw YAML content to parse.
 */
const parseYamlFile = (yamlText?: string): Array<Property> => {
  if (!yamlText) {
    return [];
  }
  const parsedYamlObject = parse(yamlText);
  return jsonToProperties(parsedYamlObject);
};

/**
 * Parses JSON text and converts it into an array of `Property` objects.
 *
 * @param text - Raw JSON string to parse.
 */
const parseJsonFile = (text?: string): Array<Property> => {
  if (!text) {
    return [];
  }
  const parsedJsonObject = JSON.parse(text);
  return jsonToProperties(parsedJsonObject);
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
  const [error, setError] = useState<Error>();
  const [isParsing, setIsParsing] = useState(false);
  const parseSequenceRef = useRef(0);

  async function parseFile (file: File) {
    const currentSequence = ++parseSequenceRef.current;
    setIsParsing(true);
    setError(undefined);

    try {
      const text = await file.text();
      const name = file.name.toLowerCase();

      let parsedProperties: Array<Property> = [];

      if (name.endsWith('.json')) {
        parsedProperties = parseJsonFile(text);
      } else if (name.endsWith('.properties')) {
        parsedProperties = parsePropertiesFile(text);
      } else if (name.endsWith('.yaml') || name.endsWith('.yml')) {
        parsedProperties = parseYamlFile(text);
      } else {
        throw new Error('Unsupported file type');
      }

      const configurationProperties: Array<ConfigurationProperty<any>> = parsedProperties.map((p: Property) => {
        return {
          name: p.name,
          state: PropertyTransitionType.ADDED,
          value: {
            encoded: p.value,
            decoded: p.value,
          },
          typeName: 'string',
          schema: {
            type: 'string',
          },
        };
      });
      if (parseSequenceRef.current !== currentSequence) {
        return;
      }
      setProperties(configurationProperties);
      setIsParsing(false);
      setError(undefined);
    } catch (e) {
      if (parseSequenceRef.current !== currentSequence) {
        return;
      }
      setProperties([]);
      setIsParsing(false);
      setError(e instanceof Error ? e : new Error('Parse error'));
    }
  }

  const reset = () => {
    parseSequenceRef.current++;
    setProperties([]);
    setError(undefined);
    setIsParsing(false);
  };

  return {
    properties,
    error,
    parseFile,
    reset,
    isParsing,
    isError: !!error,
    isReady: !isParsing && !error,
  };
}
