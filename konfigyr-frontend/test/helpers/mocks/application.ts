import type { NamespaceApplication } from '@konfigyr/hooks/types';

export const konfigyr: NamespaceApplication = {
  id: 'existing-application-id',
  name: 'konfigyr test',
  clientId: 'kfg-A9sB-6VYJWTQeJGPQsD06hfCulYfosod',
  clientSecret: undefined,
  scopes: 'namespaces:read',
  expiresAt: undefined,
};

export const createdApplication: NamespaceApplication = {
  id: 'created-application-id',
  name: 'created app',
  clientId: 'created-id',
  clientSecret: 'created-secret',
  scopes: 'namespaces:read namespaces:write',
  expiresAt: undefined,
};
