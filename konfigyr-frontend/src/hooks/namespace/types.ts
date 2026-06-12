import type { Artifact, PropertyDescriptor } from '@konfigyr/hooks/artifactory/types';

export interface Namespace {
  id: string;
  name: string;
  slug: string;
  description?: string;
  avatar?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface NamespaceOAuthScope {
  name: string;
  description?: string;
}

export interface CreateNamespace {
  name?: string;
  slug?: string;
  description?: string;
}

export interface Service {
  id: string;
  namespace: string;
  name: string;
  slug: string;
  description?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateService {
  name?: string;
  slug?: string;
  description?: string;
}

export interface Manifest {
  id: string;
  name: string;
  checksum?: string;
  artifacts: Array<Artifact>;
}

export interface ServiceCatalogProperty extends PropertyDescriptor {
  artifact: string,
}

export interface ServiceCatalog {
  service: Service,
  version?: string,
  properties: Array<ServiceCatalogProperty>
}

export type NamespaceApplicationType = 'SERVICE_ACCOUNT' | 'AGENT' | 'WORKLOAD';

export interface AgentApplicationSettings {
  type: 'agent';
  redirectUris: Array<string>;
}

export interface WorkloadApplicationSettings {
  type: 'workload';
  issuerUri: string;
  subjectPattern?: string;
}

export type NamespaceApplicationSettings = AgentApplicationSettings | WorkloadApplicationSettings | null;

export interface NamespaceApplication {
  id: string,
  name: string;
  clientId: string;
  clientSecret?: string;
  scopes: string;
  expiresAt?: string;
  type: NamespaceApplicationType;
  settings: NamespaceApplicationSettings;
}

export interface CreateNamespaceApplication {
  name: string;
  type: NamespaceApplicationType;
  scopes?: string;
  expiresAt?: string;
  settings: NamespaceApplicationSettings;
}
