import {
  booleanTransform,
  dataSizeTransform,
  dateTransform,
  durationTransform,
  numberTransform,
  stringTransform,
} from '../transforms';

import type { PropertyJsonSchema } from './types';
import type { Transform } from '../transforms';

export function useJsonSchemeTransform<T>(schema: PropertyJsonSchema): Transform<T> {
  switch (schema.type) {
    case 'boolean':
      return booleanTransform as unknown as Transform<T>;
    case 'number':
    case 'integer':
      return numberTransform as unknown as Transform<T>;
    case 'string': {
      if (schema.format === 'date-time') {
        return dateTransform as unknown as Transform<T>;
      }
      if (schema.format === 'duration') {
        return durationTransform as unknown as Transform<T>;
      }
      if (schema.format === 'data-size') {
        return dataSizeTransform as unknown as Transform<T>;
      }
      return stringTransform as unknown as Transform<T>;
    }
    default:
      return stringTransform as unknown as Transform<T>;
  }
}
