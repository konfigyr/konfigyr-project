import type { PropertyDescriptor } from '@konfigyr/hooks/artifactory/types';
import type { Namespace, Service } from '@konfigyr/hooks/namespace/types';

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

export enum PropertyTransitionType {
  ADDED = 'ADDED',
  UPDATED = 'UPDATED',
  REMOVED = 'REMOVED',
}

enum UnchangedPropertyState {
  UNCHANGED = 'UNCHANGED',
}

export type ConfigurationPropertyState = PropertyTransitionType | UnchangedPropertyState;

export const ConfigurationPropertyState = {
  ...PropertyTransitionType,
  ...UnchangedPropertyState,
};

export interface PropertyTransition {
  name: string;
  action: PropertyTransitionType;
  from?: string;
  to?: string;
}

/**
 * Interface that represents the value of a configuration property. The value contains two
 * different states, the encoded and the decoded.
 *
 * The encoded value is the value managed by the Konfigyr Vault, and it is always represented
 * by a string. The decoded value is the value * used by the UI presentation layer.
 *
 * For example, the decoded value could be a boolean value, a number, or a complex object like
 * a duration, a date, or a list of strings.
 */
export interface ConfigurationPropertyValue<T> {
  encoded: string;
  decoded: T;
}

export interface ConfigurationProperty<T> extends PropertyDescriptor {
  value?: ConfigurationPropertyValue<T>;
  state: ConfigurationPropertyState;
}

export interface ChangesetState {
  namespace: Namespace;
  service: Service;
  profile: Profile;
  name: string;
  description?: string;
  state: string;
  properties: Array<ConfigurationProperty<any>>;
  added: number;
  modified: number;
  deleted: number;
}

export enum Operation {
  CREATE = 'CREATE',
  MODIFY = 'MODIFY',
  REMOVE = 'REMOVE',
}

/**
 * Interface that represents the latest revision information of a vault when a change
 * is applied or a change request is merged.
 */
export interface VaultRevisionInformation {
  revision: string;
  author: string;
  subject: string;
  description?: string;
  timestamp: string;
}

export interface PropertyChange {
  name: string;
  operation: Operation;
  value?: string;
}

export interface ApplyRequest {
  name: string;
  description?: string;
  changes: Array<PropertyChange>;
}

export enum ChangeRequestState {
  OPEN = 'OPEN',
  MERGED = 'MERGED',
  DISCARDED = 'DISCARDED',
}

export enum ChangeRequestMergeStatus {
  NOT_OPEN = 'NOT_OPEN',
  OUTDATED = 'OUTDATED',
  CONFLICTING = 'CONFLICTING',
  NOT_APPROVED = 'NOT_APPROVED',
  CHANGES_REQUESTED = 'CHANGES_REQUESTED',
  CHECKING = 'CHECKING',
  MERGEABLE = 'MERGEABLE',
}

export interface ChangeRequest {
  id: string;
  service: Service;
  profile: Profile;
  number: number;
  state: ChangeRequestState;
  mergeStatus: ChangeRequestMergeStatus
  subject: string;
  description?: string;
  count: number;
  createdBy: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ChangeRequestChange extends PropertyTransition {
}

export enum ChangeRequestReviewType {
  COMMENT = 'COMMENT',
  APPROVE = 'APPROVE',
  REQUEST_CHANGES = 'REQUEST_CHANGES',
}

export interface SubmitChangeRequestReview {
  state: ChangeRequestReviewType;
  comment?: string;
}

export enum ChangeRequestHistoryType {
  CREATED = 'CREATED',
  APPROVED = 'APPROVED',
  COMMENTED = 'COMMENTED',
  CHANGES_REQUESTED = 'CHANGES_REQUESTED',
  RENAMED = 'RENAMED',
  MERGED = 'MERGED',
  DISCARDED = 'DISCARDED',
}

export interface ChangeRequestHistory {
  id: string;
  type: ChangeRequestHistoryType;
  initiator: string;
  timestamp: string;
}

export interface ChangeRequestQuery extends Record<string, string | number | boolean | undefined> {
  term?: string;
  profile?: string;
  state?: ChangeRequestState;
  page?: number;
  size?: number;
}

export interface ChangeHistory {
  id: string;
  revision: string;
  subject: string;
  description?: string;
  count: number;
  appliedBy: string;
  appliedAt: string;
}

export interface ChangeHistoryRecord extends PropertyTransition {
  id: string;
  revision: string;
  appliedBy: string;
  appliedAt: string;
}

export interface ChangeHistoryQuery extends Record<string, string | number | boolean | undefined> {
  size?: number;
  token?: string;
}
