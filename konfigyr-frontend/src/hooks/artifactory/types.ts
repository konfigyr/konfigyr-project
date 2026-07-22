import type { Pageable } from '@konfigyr/hooks/hateoas/types';

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

export type ArtifactVisibility = 'PUBLIC' | 'PRIVATE';

export interface ArtifactDefinition {
  id: string;
  groupId: string;
  artifactId: string;
  visibility: ArtifactVisibility;
  name?: string;
  description?: string;
  website?: string;
  repository?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface VersionedArtifact {
  id: string;
  groupId: string;
  artifactId: string;
  version: string;
  visibility: ArtifactVisibility;
  name?: string;
  description?: string;
  website?: string;
  repository?: string;
  publishedAt: string;
}

export interface ArtifactQuery extends Pageable {
  term?: string;
}

export interface ArtifactVersionQuery extends Pageable {
  term?: string;
}

export interface PropertySearchQuery extends Pageable {
  groupId: string;
  artifactId: string;
  version?: string;
  term: string;
}

export interface ChangeArtifactVisibilityPayload {
  groupId: string;
  artifactId: string;
  visibility: ArtifactVisibility;
}

export interface PropertyJsonSchema {
  type: 'string' | 'number' | 'integer' | 'boolean' | 'object' | 'array' | 'null';
  title?: string;
  description?: string;
  default?: string;
  deprecated?: boolean;
  enum?: Array<string>;
  examples?: Array<string>;

  /* Used by both numeric and string types */
  format?: string;

  /* Numeric types */
  multipleOf?: number;
  maximum?: number;
  exclusiveMaximum?: number;
  minimum?: number;
  exclusiveMinimum?: number;

  /* String type */
  maxLength?: number;
  minLength?: number;
  pattern?: string;

  /* Array type */
  items?: PropertyJsonSchema;
  maxItems?: number;
  minItems?: number;
  uniqueItems?: boolean;

  /* Object type */
  required?: Array<string>;
  properties?: Record<string, PropertyJsonSchema>;
  propertyNames?: PropertyJsonSchema;
  additionalProperties?: PropertyJsonSchema;
}

export interface PropertyDeprecation {
  reason?: string;
  replacement?: string;
}

export interface PropertyDescriptor {
  name: string;
  typeName: string;
  schema: PropertyJsonSchema;
  description?: string;
  deprecation?: PropertyDeprecation;
  defaultValue?: string;
}
