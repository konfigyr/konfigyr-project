import { queryOptions, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import request from '@konfigyr/lib/http';

import type { PageResponse } from '@konfigyr/hooks/hateoas/types';
import type {
  ArtifactDefinition,
  ArtifactQuery,
  ArtifactVersionQuery,
  ChangeArtifactVisibilityPayload,
  PropertyDescriptor,
  PropertySearchQuery,
  VersionedArtifact,
} from './types';

export const registryKeys = {
  list: (namespace: string, query: ArtifactQuery) => ['namespace', namespace, 'registry', 'artifacts', query],
  get: (namespace: string, groupId: string, artifactId: string) => ['namespace', namespace, 'registry', 'artifacts', groupId, artifactId],
  versions: (namespace: string, groupId: string, artifactId: string, query: ArtifactVersionQuery) => [
    ...registryKeys.get(namespace, groupId, artifactId), 'versions', query,
  ],
  version: (namespace: string, groupId: string, artifactId: string, version: string) => [
    ...registryKeys.get(namespace, groupId, artifactId), version,
  ],
  properties: (namespace: string, query: PropertySearchQuery) => ['namespace', namespace, 'registry', 'properties', query],
};

export const getArtifacts = (namespace: string, query: ArtifactQuery = {}) => {
  return queryOptions({
    queryKey: registryKeys.list(namespace, query),
    queryFn: async ({ signal }): Promise<PageResponse<ArtifactDefinition>> => {
      return await request
        .get(`api/namespaces/${namespace}/artifacts`, { signal, searchParams: { ...query } })
        .json();
    },
    placeholderData: previousData => previousData,
  });
};

export const getArtifact = (namespace: string, groupId: string, artifactId: string) => {
  return queryOptions({
    queryKey: registryKeys.get(namespace, groupId, artifactId),
    queryFn: async ({ signal }): Promise<ArtifactDefinition> => {
      return await request
        .get(`api/namespaces/${namespace}/artifacts/${groupId}/${artifactId}`, { signal })
        .json();
    },
  });
};

export const getArtifactVersions = (namespace: string, groupId: string, artifactId: string, query: ArtifactVersionQuery = {}) => {
  return queryOptions({
    queryKey: registryKeys.versions(namespace, groupId, artifactId, query),
    queryFn: async ({ signal }): Promise<PageResponse<VersionedArtifact>> => {
      return await request
        .get(`api/namespaces/${namespace}/artifacts/${groupId}/${artifactId}/versions`, { signal, searchParams: { ...query } })
        .json();
    },
    placeholderData: previousData => previousData,
  });
};

export const getArtifactVersion = (namespace: string, groupId: string, artifactId: string, version: string) => {
  return queryOptions({
    queryKey: registryKeys.version(namespace, groupId, artifactId, version),
    queryFn: async ({ signal }): Promise<VersionedArtifact> => {
      return await request
        .get(`api/namespaces/${namespace}/artifacts/${groupId}/${artifactId}/${version}`, { signal })
        .json();
    },
  });
};

export const searchArtifactProperties = (namespace: string, query: PropertySearchQuery) => {
  return queryOptions({
    queryKey: registryKeys.properties(namespace, query),
    queryFn: async ({ signal }): Promise<PageResponse<PropertyDescriptor>> => {
      return await request
        .get(`api/namespaces/${namespace}/artifacts/search`, { signal, searchParams: { ...query } })
        .json();
    },
    enabled: query.term.trim().length > 0,
    placeholderData: previousData => previousData,
  });
};

export const useGetArtifacts = (namespace: string, query?: ArtifactQuery) => {
  return useQuery(getArtifacts(namespace, query));
};

export const useGetArtifact = (namespace: string, groupId: string, artifactId: string) => {
  return useQuery(getArtifact(namespace, groupId, artifactId));
};

export const useGetArtifactVersions = (namespace: string, groupId: string, artifactId: string, query?: ArtifactVersionQuery) => {
  return useQuery(getArtifactVersions(namespace, groupId, artifactId, query));
};

export const useGetArtifactVersion = (namespace: string, groupId: string, artifactId: string, version: string) => {
  return useQuery(getArtifactVersion(namespace, groupId, artifactId, version));
};

export const useSearchArtifactProperties = (namespace: string, query: PropertySearchQuery) => {
  return useQuery(searchArtifactProperties(namespace, query));
};

export const useChangeArtifactVisibility = (namespace: string) => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: async (payload: ChangeArtifactVisibilityPayload): Promise<void> => {
      await request.put(`api/namespaces/${namespace}/artifacts/${payload.groupId}/${payload.artifactId}/visibility`, {
        json: { visibility: payload.visibility },
      });
    },
    onSuccess: async (_, payload) => {
      client.setQueryData(
        registryKeys.get(namespace, payload.groupId, payload.artifactId),
        (artifact?: ArtifactDefinition) => artifact && { ...artifact, visibility: payload.visibility },
      );
      await client.invalidateQueries({
        queryKey: ['namespace', namespace, 'registry', 'artifacts'],
      });
    },
  });
};
