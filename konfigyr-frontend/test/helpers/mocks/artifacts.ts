import type { ArtifactDefinition, PropertyDescriptor, VersionedArtifact } from '@konfigyr/hooks/artifactory/types';

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

export const publicArtifactProperties: Array<PropertyDescriptor> = [{
  name: 'example.timeout',
  typeName: 'java.time.Duration',
  schema: { type: 'string', format: 'duration' },
  description: 'Timeout duration for example service calls.',
  defaultValue: '30s',
}, {
  name: 'example.retries',
  typeName: 'java.lang.Integer',
  schema: { type: 'integer' },
  description: 'Number of retries before failing.',
  defaultValue: '3',
}];
