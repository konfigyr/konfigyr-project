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
