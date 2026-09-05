import type { NamespaceApplication } from '@konfigyr/hooks/types';

export const konfigyr: NamespaceApplication = {
  id: 'existing-application-id',
  name: 'konfigyr test',
  clientId: 'kfg-A9sB-6VYJWTQeJGPQsD06hfCulYfosod',
  clientSecret: undefined,
  scopes: 'namespaces:read',
  expiresAt: undefined,
  type: 'SERVICE_ACCOUNT',
  settings: null,
};

export const createdApplication: NamespaceApplication = {
  id: 'created-application-id',
  name: 'created app',
  clientId: 'created-id',
  clientSecret: 'created-secret',
  scopes: 'namespaces:read namespaces:write',
  expiresAt: undefined,
  type: 'SERVICE_ACCOUNT',
  settings: null,
};

export const agentApplication: NamespaceApplication = {
  id: 'agent-application-id',
  name: 'konfigyr agent',
  clientId: 'kfg-agent-client-id',
  clientSecret: undefined,
  scopes: 'namespaces:read',
  expiresAt: undefined,
  type: 'AGENT',
  settings: {
    type: 'agent' as const,
    redirectUris: ['https://example.com/callback', 'https://other.example.com/callback'],
  },
};

export const workloadApplication: NamespaceApplication = {
  id: 'workload-application-id',
  name: 'konfigyr workload',
  clientId: 'kfg-workload-client-id',
  clientSecret: undefined,
  scopes: 'namespaces:read',
  expiresAt: undefined,
  type: 'WORKLOAD',
  settings: {
    type: 'workload' as const,
    issuerUri: 'https://token.actions.githubusercontent.com',
    subjectPattern: 'repo:owner/name:ref:refs/heads/main',
  },
};
