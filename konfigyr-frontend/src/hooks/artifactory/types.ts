import type { JSONSchema7 } from 'json-schema';

export interface Artifact {
  id: string;
  groupId: string;
  artifactId: string;
  version: string;
  name?: string;
  description?: string;
  website?: string;
  repository?: string;
}

export interface PropertyDeprecation {
  reason: string;
  replacement?: string;
}

export interface PropertyDescriptor {
  name: string;
  typeName: string;
  description?: string;
  schema?: JSONSchema7;
  deprecation?: PropertyDeprecation;
  defaultValue?: string;
}

