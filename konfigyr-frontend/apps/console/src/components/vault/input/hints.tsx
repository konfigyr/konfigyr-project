import { useId } from 'react';

import type { ComponentProps } from 'react';
import type { PropertyJsonSchema } from '@konfigyr/hooks/artifactory/types';
import type { SchemaHint } from './types';

export interface SchemaHintProps {
  id: string;
  hints: Array<SchemaHint>;
}

const COMMON_CHARSET_HINTS = [
  'UTF-8', // Used by ~99% of websites
  'UTF-16', // Common in internal application processing
  'ISO-8859-1', // Latin-1 - Western European
  'US-ASCII', // Basic English
  'Windows-1252', // Standard Windows Western
].map(value => ({ value, label: value }));

const COMMON_LANGUAGE_HINTS = [{
  value: 'en-US', label: 'en-US - English, United States',
}, {
  value: 'en-GB', label: 'en-GB - English, United Kingdom',
}, {
  value: 'zh-CN', label: 'zh-CN - Chinese, Simplified',
}, {
  value: 'es-ES', label: 'es-ES - Spanish, Spain',
}, {
  value: 'fr-FR', label: 'fr-FR - French, France',
}, {
  value: 'de-DE', label: 'de-DE - German, Germany',
}, {
  value: 'ja-JP', label: 'ja-JP - Japanese, Japan',
}, {
  value: 'pt-BR', label: 'pt-BR - Portuguese, Brazil',
}, {
  value: 'it-IT', label: 'it-IT - Italian, Italy',
}, {
  value: 'ko-KR', label: 'ko-KR - Korean, South Korea',
}];

const COMMON_CONTENT_TYPE_HINTS = [
  'text/html',
  'text/plain',
  'application/json',
  'application/pdf',
  'image/jpeg',
  'image/png',
  'audio/mpeg',
  'video/mp4',
  'application/zip',
  'application/octet-stream',
].map(value => ({ value, label: value }));

const COMMON_TIMEZONE_HINTS = [{
  value: 'UTC', label: 'UTC - Coordinated Universal Time',
}, {
  value: 'America/New_York', label: 'America/New York - Eastern Time',
}, {
  value: 'America/Chicago', label: 'America/Chicago - Central Time',
}, {
  value: 'America/Los_Angeles', label: 'America/Los Angeles - Pacific Time',
}, {
  value: 'Europe/London', label: 'Europe/London - GMT/BST',
}, {
  value: 'Europe/Berlin', label: 'Europe/Berlin - Central European Time',
}, {
  value: 'Asia/Tokyo', label: 'Asia/Tokyo - Japan Standard Time',
}, {
  value: 'Asia/Seoul', label: 'Asia/Seoul - Korea Standard Time',
}, {
  value: 'Asia/Kolkata', label: 'Asia/Kolkata - India Standard Time',
}];

const generateSchemaHints = (schema: PropertyJsonSchema): Array<SchemaHint> => {
  if (schema.examples && schema.examples.length > 0) {
    return schema.examples.map(value => ({ value, label: value }));
  }

  if (schema.type === 'array') {
    return generateSchemaHints(schema.items as PropertyJsonSchema);
  }

  if (schema.format === 'charset') {
    return [...COMMON_CHARSET_HINTS];
  }

  if (schema.format === 'language') {
    return [...COMMON_LANGUAGE_HINTS];
  }

  if (schema.format === 'mime-type') {
    return [...COMMON_CONTENT_TYPE_HINTS];
  }

  if (schema.format === 'time-zone') {
    return [...COMMON_TIMEZONE_HINTS];
  }

  return [];
};

/**
 * Hook to generate schema hints for a given JSON schema. This hook will generate a list of suggestions
 * and a unique ID for the datalist element. The returned value can be used to render a datalist element
 * via the `SchemaHints` component.
 *
 * @param schema the JSON schema to generate hints for
 * @returns an object containing the ID and the list of hints, or `null` if no hints are available
 */
export const useSchemaHints = (schema: PropertyJsonSchema): SchemaHintProps | null => {
  const id = useId();
  const hints = generateSchemaHints(schema);

  if (hints.length === 0) {
    return null;
  }

  return { id, hints };
};

export function SchemaHints({ id, hints, ...props }: SchemaHintProps & ComponentProps<'datalist'>) {
  return (
    <datalist id={id} {...props}>
      {hints.map(hint => (
        <option key={hint.value} value={hint.value}>
          {hint.label}
        </option>
      ))}
    </datalist>
  );
}
