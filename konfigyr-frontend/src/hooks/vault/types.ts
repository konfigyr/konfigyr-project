import type { Namespace, Service } from '@konfigyr/hooks/namespace/types';
import type { JSONSchema7 } from 'json-schema';

export type ProfilePolicy = 'UNPROTECTED' | 'PROTECTED' | 'IMMUTABLE';

export interface Profile {
  id: string;
  slug: string;
  name: string;
  policy: ProfilePolicy;
  position: number;
}

export interface CreateProfile {
  slug?: string;
  name?: string;
  policy?: string;
  position?: number;
}

export interface PropertyDeprecation {
  reason: string;
  replacement?: string;
}

export type ConfigurationPropertyState = 'unchanged' | 'modified' | 'deleted' | 'added';

export interface PropertyDescriptor {
  name: string;
  type: string;
  description?: string;
  schema?: JSONSchema7;
  deprecation?: PropertyDeprecation;
}

export interface ConfigurationProperty extends PropertyDescriptor {
  value?: string
  state: ConfigurationPropertyState
}

export interface ChangesetState {
  namespace: Namespace;
  service: Service;
  profile: Profile;
  name: string;
  state: string;
  properties: Array<ConfigurationProperty>;
  added: number;
  modified: number;
  deleted: number;
}

export interface ChangeHistoryRecord {
  id: string;
  user: string;
  action: 'created' | 'modified' | 'deleted';
  previousValue?: string;
  newValue?: string;
  timestamp: string;
}

export enum Operation {
  CREATE = 'CREATE',
  MODIFY = 'MODIFY',
  REMOVE = 'REMOVE',
}

export interface ApplyResult {
  revision: string;
  changes: Map<string, ChangeHistoryRecord>
}

export interface PropertyChange {
  name: string,
  operation: Operation,
  value?: string,
}

export interface ApplyRequest {
  name: string,
  description?: string,
  changes: Array<PropertyChange>
}

export interface ChangeHistory {
  id: string;
  subject: string;
  description: string;
  appliedBy: string;
  appliedAt: string;
}

export interface ChangeHistoryQuery extends Record<string, string | number | boolean | undefined> {
  size?: number;
  page?: number;
}
