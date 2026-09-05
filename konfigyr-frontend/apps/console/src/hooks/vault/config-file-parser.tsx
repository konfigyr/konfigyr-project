import { useRef, useState } from 'react';
import { PropertyTransitionType } from '@konfigyr/hooks/vault/types';
import { parse } from 'yaml';
import { fetchSpringConfigHandler } from '@konfigyr/hooks/vault/-handler';
import type { FetchConfigRequest } from '@konfigyr/hooks/vault/-handler';
import type { ConfigurationProperty } from '@konfigyr/hooks/vault/types';

/**
 * Builds a configuration property object from a name/value pair.
 *
 * @param name - property key.
 * @param value - property value
 */
const buildConfigurationProperty = (name: string, value: string): ConfigurationProperty<string> => {
  return {
    name: name,
    state: PropertyTransitionType.ADDED,
    value: {
      encoded: value,
      decoded: value,
    },
    typeName: 'string',
    schema: {
      type: 'string',
    },
  };
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
const parsePropertiesFile = (text: string): Array<ConfigurationProperty<string>> => {
  const out: Array<ConfigurationProperty<string>> = [];

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
        buildConfigurationProperty(key, value),
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
function jsonToProperties (obj: unknown): Array<ConfigurationProperty<string>> {
  const properties: Array<ConfigurationProperty<string>> = [];

  const append = (value: unknown, prefix: string) => {
    if (!prefix) {
      return;
    }

    if (Array.isArray(value)) {
      if (value.every(isScalar)) {
        properties.push(
          buildConfigurationProperty(prefix, value.map(v => String(v)).join(',')),
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
      buildConfigurationProperty(prefix, String(value)),
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
const parseYamlFile = (yamlText?: string): Array<ConfigurationProperty<string>> => {
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
const parseJsonFile = (text?: string): Array<ConfigurationProperty<string>> => {
  if (!text) {
    return [];
  }
  const parsedJsonObject = JSON.parse(text);
  return jsonToProperties(parsedJsonObject);
};

/**
 * Parses Spring Cloud Config server response and converts effective `propertySources[*].source` entries into flat configuration properties.
 *
 * @param payload - Raw JSON payload returned by Spring Cloud Config server
 */
const parseSpringCloudConfigResponse = (payload: unknown): Array<ConfigurationProperty<string>> => {
  if (!payload || typeof payload !== 'object') {
    throw new Error('Invalid configuration response');
  }

  const propertySources = (payload as { propertySources?: unknown }).propertySources;
  if (!Array.isArray(propertySources)) {
    throw new Error('Invalid configuration response: missing propertySources');
  }

  const mergedSource: Record<string, unknown> = {};

  // Spring Cloud returns sources in precedence order (highest first).
  // Merge from last to first so higher-precedence sources override lower ones.
  for (let i = propertySources.length - 1; i >= 0; i--) {
    const source = (propertySources[i] as { source?: unknown }).source;
    if (source && typeof source === 'object' && !Array.isArray(source)) {
      Object.assign(mergedSource, source);
    }
  }

  return jsonToProperties(mergedSource);
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
 * - `parseFile` - Async function to parse a given `File`.
 * - `fetchConfig` - Async function to fetch JSON configuration from the config server API and parse it.
 * - `reset` - Function to clear parsed data and errors.
 * - `isParsing` - Indicates whether a parse operation is currently in progress.
 * - `isError` - Indicates whether the latest parse operation failed.
 * - `isReady` - Indicates parser idle state (not parsing and no error).
 */
export function useConfigFileParser () {
  const [properties, setProperties] = useState<Array<ConfigurationProperty<any>>>([]);
  const [error, setError] = useState<Error>();
  const [isParsing, setIsParsing] = useState(false);
  const parseSequenceRef = useRef(0);

  const runParseTask = async (task: () => Promise<Array<ConfigurationProperty<string>>>) => {
    const currentSequence = ++parseSequenceRef.current;
    setIsParsing(true);
    setError(undefined);

    try {
      const parsedProperties = await task();

      if (parseSequenceRef.current !== currentSequence) {
        return;
      }
      setProperties(parsedProperties);
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
  };

  const parseFile = async (file: File)=> {
    await runParseTask(async () => {
      const text = await file.text();
      const name = file.name.toLowerCase();

      if (name.endsWith('.json')) {
        return parseJsonFile(text);
      }
      if (name.endsWith('.properties')) {
        return parsePropertiesFile(text);
      }
      if (name.endsWith('.yaml') || name.endsWith('.yml')) {
        return parseYamlFile(text);
      }

      throw new Error('Unsupported file type');
    });
  };

  const fetchConfig = async (payload: FetchConfigRequest) => {
    await runParseTask(async () => {
      const response = await fetchSpringConfigHandler({
        data: payload,
      });
      return parseSpringCloudConfigResponse(response);
    });
  };

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
    fetchConfig,
    reset,
    isParsing,
    isError: !!error,
    isReady: !isParsing && !error,
  };
}
