import { johnDoe, konfigyr } from './namespace';

import type { ArtifactDefinition, ArtifactVisibility, PropertyDefinition, VersionedArtifact } from '@konfigyr/hooks/artifactory/types';

export const publicArtifact: ArtifactDefinition = {
  id: 'artifact-public',
  groupId: 'com.example',
  artifactId: 'public-service',
  visibility: 'PUBLIC',
  name: 'Public Service',
  description: 'A publicly readable artifact.',
  website: 'https://example.com',
  repository: 'https://github.com/example/public-service',
  createdAt: '2026-06-01T00:00:00Z',
  updatedAt: '2026-07-10T00:00:00Z',
};

export const privateArtifact: ArtifactDefinition = {
  id: 'artifact-private',
  groupId: 'com.example',
  artifactId: 'private-service',
  visibility: 'PRIVATE',
  name: 'Private Service',
  description: 'A private artifact only readable by its owning namespace.',
  createdAt: '2026-06-05T00:00:00Z',
  updatedAt: '2026-07-11T00:00:00Z',
};

export const singleVersionArtifact: ArtifactDefinition = {
  id: 'artifact-single-version',
  groupId: 'com.example',
  artifactId: 'single-version-service',
  visibility: 'PUBLIC',
  name: 'Single Version Service',
  createdAt: '2026-06-06T00:00:00Z',
  updatedAt: '2026-06-06T00:00:00Z',
};

export const publicArtifactVersionOne: VersionedArtifact = {
  id: 'version-public-1',
  groupId: publicArtifact.groupId,
  artifactId: publicArtifact.artifactId,
  version: '1.0.0',
  visibility: 'PUBLIC',
  name: publicArtifact.name,
  publishedAt: '2026-06-01T00:00:00Z',
};

export const publicArtifactVersionTwo: VersionedArtifact = {
  id: 'version-public-2',
  groupId: publicArtifact.groupId,
  artifactId: publicArtifact.artifactId,
  version: '2.0.0',
  visibility: 'PUBLIC',
  name: publicArtifact.name,
  publishedAt: '2026-07-10T00:00:00Z',
};

export const singleVersion: VersionedArtifact = {
  id: 'version-single',
  groupId: singleVersionArtifact.groupId,
  artifactId: singleVersionArtifact.artifactId,
  version: '1.0.0',
  visibility: 'PUBLIC',
  publishedAt: '2026-06-06T00:00:00Z',
};

export const publicArtifactProperties: Array<PropertyDefinition> = [{
  id: 'property-example-timeout',
  key: `${publicArtifact.groupId}:${publicArtifact.artifactId}`,
  owner: { id: konfigyr.id, slug: konfigyr.slug },
  checksum: 'checksum-example-timeout',
  occurrences: 2,
  firstSeen: publicArtifactVersionOne.version,
  lastSeen: publicArtifactVersionTwo.version,
  name: 'example.timeout',
  typeName: 'java.time.Duration',
  schema: { type: 'string', format: 'duration' },
  description: 'Timeout duration for example service calls.',
  defaultValue: '30s',
}, {
  id: 'property-example-retries',
  key: `${publicArtifact.groupId}:${publicArtifact.artifactId}`,
  owner: { id: konfigyr.id, slug: konfigyr.slug },
  checksum: 'checksum-example-retries',
  occurrences: 2,
  firstSeen: publicArtifactVersionOne.version,
  lastSeen: publicArtifactVersionTwo.version,
  name: 'example.retries',
  typeName: 'java.lang.Integer',
  schema: { type: 'integer' },
  description: 'Number of retries before failing.',
  defaultValue: '3',
}];

export const otherNamespacePublicArtifact: ArtifactDefinition = {
  id: 'artifact-other-public',
  groupId: 'io.johndoe',
  artifactId: 'notes-service',
  visibility: 'PUBLIC',
  name: 'Notes Service',
  description: 'A public artifact owned by a different namespace.',
  createdAt: '2026-05-01T00:00:00Z',
  updatedAt: '2026-05-01T00:00:00Z',
};

export const otherNamespacePrivateArtifact: ArtifactDefinition = {
  id: 'artifact-other-private',
  groupId: 'io.johndoe',
  artifactId: 'secret-service',
  visibility: 'PRIVATE',
  name: 'Secret Service',
  description: 'A private artifact owned by a different namespace.',
  createdAt: '2026-05-02T00:00:00Z',
  updatedAt: '2026-05-02T00:00:00Z',
};

/**
 * Fixtures for the platform-wide property search endpoint. `visibility` is not part of the
 * `PropertyDefinition` API contract itself, it exists here only so the MSW `search` handler
 * can apply the same visibility rule the real backend enforces (PUBLIC everywhere, PRIVATE only
 * to its owning namespace).
 */
export const platformSearchProperties: Array<PropertyDefinition & { visibility: ArtifactVisibility }> = [{
  id: 'property-platform-own-public',
  key: `${publicArtifact.groupId}:${publicArtifact.artifactId}`,
  owner: { id: konfigyr.id, slug: konfigyr.slug },
  visibility: 'PUBLIC',
  checksum: 'checksum-platform-own-public',
  occurrences: 1,
  firstSeen: publicArtifactVersionOne.version,
  lastSeen: publicArtifactVersionTwo.version,
  name: 'example.timeout',
  typeName: 'java.time.Duration',
  schema: { type: 'string', format: 'duration' },
  description: 'Timeout duration for example service calls.',
  defaultValue: '30s',
}, {
  id: 'property-platform-own-private',
  key: `${privateArtifact.groupId}:${privateArtifact.artifactId}`,
  owner: { id: konfigyr.id, slug: konfigyr.slug },
  visibility: 'PRIVATE',
  checksum: 'checksum-platform-own-private',
  occurrences: 1,
  firstSeen: '1.0.0',
  lastSeen: '1.0.0',
  name: 'internal.secret.rotation-interval',
  typeName: 'java.time.Duration',
  schema: { type: 'string', format: 'duration' },
  description: 'How often internal secrets rotate.',
}, {
  id: 'property-platform-other-public',
  key: `${otherNamespacePublicArtifact.groupId}:${otherNamespacePublicArtifact.artifactId}`,
  owner: { id: johnDoe.id, slug: johnDoe.slug },
  visibility: 'PUBLIC',
  checksum: 'checksum-platform-other-public',
  occurrences: 1,
  firstSeen: '1.0.0',
  lastSeen: '1.0.0',
  name: 'notes.storage.path',
  typeName: 'java.lang.String',
  schema: { type: 'string' },
  description: 'Filesystem path where notes are stored.',
}, {
  id: 'property-platform-other-private',
  key: `${otherNamespacePrivateArtifact.groupId}:${otherNamespacePrivateArtifact.artifactId}`,
  owner: { id: johnDoe.id, slug: johnDoe.slug },
  visibility: 'PRIVATE',
  checksum: 'checksum-platform-other-private',
  occurrences: 1,
  firstSeen: '1.0.0',
  lastSeen: '1.0.0',
  name: 'secret.internal.token',
  typeName: 'java.lang.String',
  schema: { type: 'string' },
  description: 'Internal token, must never leak to other namespaces.',
}];
