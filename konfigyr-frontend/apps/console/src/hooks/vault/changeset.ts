import { useMemo } from 'react';
import { keepPreviousData, queryOptions, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useServiceCatalogQuery } from '@konfigyr/hooks/namespace/query';
import { useChangesetStorage } from '@konfigyr/hooks/vault/changeset-storage';

import request from '@konfigyr/lib/http';
import { resolveJsonSchemeTransform } from '@konfigyr/hooks/artifactory/hooks';
import { ConfigurationPropertyState, Operation } from '@konfigyr/hooks/vault/types';

import type { PropertyDescriptor } from '@konfigyr/hooks/artifactory/types';
import type { ChangesetStorageState } from '@konfigyr/hooks/vault/changeset-storage';
import type { CollectionResponse, CursorResponse } from '@konfigyr/hateoas';
import type { Namespace, Service, ServiceCatalog } from '@konfigyr/hooks/namespace/types';
import type {
  ApplyRequest,
  ChangeHistory,
  ChangeHistoryQuery,
  ChangeHistoryRecord,
  ChangeRequest,
  ChangesetState,
  ConfigurationProperty,
  ConfigurationPropertyValue,
  Profile,
  PropertyChange,
  VaultRevisionInformation,
} from '@konfigyr/hooks/vault/types';

const DEFAULT_PROPERTY_DESCRIPTOR: Omit<PropertyDescriptor, 'name'> = {
  typeName: 'java.lang.String',
  schema: { type: 'string' },
};

/**
 * Keys used to store the Changeset states in the query client.
 */
export const vaultKeys = {
  getChangeset: (profile: Profile) => ['vault', profile.id, 'changeset'],
  getChangeHistory: (profile: Profile, query?: ChangeHistoryQuery) => ['vault', profile.id, 'history-query', query],
  getChangeHistoryDetails: (profile: Profile, history: ChangeHistory) => ['vault', profile.id, 'history', history.id],
};

const countChanges = (properties: Array<ConfigurationProperty<any>>) => ({
  added: properties.filter(it => it.state === ConfigurationPropertyState.ADDED).length,
  modified: properties.filter(it => it.state === ConfigurationPropertyState.UPDATED).length,
  deleted: properties.filter(it => it.state === ConfigurationPropertyState.REMOVED).length,
});

const EMPTY_CHANGESET_STORAGE_STATE: ChangesetStorageState = {
  name: 'Changeset draft',
  properties: [],
};

const upsertChangesetProperty = (
  draft: ChangesetStorageState | null | undefined,
  property: ConfigurationProperty<any>,
): ChangesetStorageState => {
  const state = draft ?? EMPTY_CHANGESET_STORAGE_STATE;

  return {
    ...state,
    properties: [...state.properties.filter(it => it.name !== property.name), property],
  };
};

const generateApplyRequestFromChangeset = (payload: ChangesetState): ApplyRequest => {
  const changes = payload.properties.reduce((acc, property) => {
    const { name, value } = property;

    switch (property.state) {
      case ConfigurationPropertyState.ADDED:
        return [...acc, { name, value: value?.encoded, operation: Operation.CREATE }];
      case ConfigurationPropertyState.UPDATED:
        return [...acc, { name, value: value?.encoded, operation: Operation.MODIFY }];
      case ConfigurationPropertyState.REMOVED:
        return [...acc, { name, operation: Operation.REMOVE }];
      default:
        return acc;
    }
  }, [] as Array<PropertyChange>);

  return {
    name: payload.name,
    description: payload.description,
    changes,
  };
};

const generateStubChangesetState = (
  namespace: Namespace,
  service: Service,
  profile: Profile,
  catalog: ServiceCatalog,
  values: Record<string, string>,
): ChangesetState => {
  const properties = Object.keys(values).reduce((state, name) => {
    const descriptor = catalog.properties.find(it => it.name === name);

    // resolve the transform function for the property descriptor and construct the property value
    const transform = resolveJsonSchemeTransform<any>(descriptor?.schema ?? DEFAULT_PROPERTY_DESCRIPTOR.schema);
    const encoded = values[name];
    const decoded = transform.decode(encoded);

    const property: ConfigurationProperty<any> = {
      ...(descriptor || DEFAULT_PROPERTY_DESCRIPTOR),
      name: name,
      value: { encoded, decoded },
      state: ConfigurationPropertyState.UNCHANGED,
    };

    return [...state, property];
  }, [] as Array<ConfigurationProperty<any>>);

  return {
    namespace,
    service,
    profile,
    name: 'Changeset draft',
    state: 'DRAFT',
    properties,
    ...countChanges(properties),
  };
};

/**
 * Applies the changes from the changeset storage state on top of the changeset state created from the server values.
 *
 * Properties without changes keep their server value. Removed properties that no longer exist on the server
 * are dropped. Added or updated properties are marked as updated when the property exists on the server,
 * otherwise they are marked as added. Changed properties whose value matches the server value are unchanged.
 *
 * @param base changeset state created from the current server values
 * @param draft changeset storage state with the changes made by the user
 * @returns changeset state with the changes applied
 */
export const rebaseChangeset = (base: ChangesetState, draft: ChangesetStorageState | null): ChangesetState => {
  if (!draft) {
    return base;
  }

  const changes = new Map(draft.properties
    .filter(property => property.state !== ConfigurationPropertyState.UNCHANGED)
    .map(property => [property.name, property] as const));

  const properties = base.properties.map(property => {
    const change = changes.get(property.name);

    if (!change) {
      return property;
    }

    changes.delete(property.name);

    if (change.state === ConfigurationPropertyState.REMOVED) {
      return { ...property, state: ConfigurationPropertyState.REMOVED };
    }

    if (change.value?.encoded === property.value?.encoded) {
      return property;
    }

    return { ...property, value: change.value, state: ConfigurationPropertyState.UPDATED };
  });

  changes.forEach(change => {
    if (change.state !== ConfigurationPropertyState.REMOVED) {
      properties.push({ ...change, state: ConfigurationPropertyState.ADDED });
    }
  });

  return {
    ...base,
    name: draft.name,
    description: draft.description,
    properties,
    ...countChanges(properties),
  };
};

/**
 * Retrieves the current configuration property values of the given profile as key/value pairs.
 *
 * @param namespace namespace that owns this service
 * @param service single Spring Boot application or microservice that belongs to a given namespace
 * @param profile profile for which the property values are being retrieved
 * @returns TanStack query options to retrieve the profile property values
 */
export const getProfilePropertiesQuery = (namespace: Namespace, service: Service, profile: Profile) => {
  return queryOptions({
    queryKey: vaultKeys.getChangeset(profile),
    queryFn: ({ signal }): Promise<Record<string, string>> => {
      return request.get(`api/namespaces/${namespace.slug}/services/${service.slug}/profiles/${profile.slug}/properties`, { signal })
        .json<Record<string, string>>();
    },
  });
};

/**
 * Hook that retrieves the current changeset state for the given profile and current user account.
 *
 * The changeset state is created from the current server values with the changes from the changeset
 * storage applied on top of them. Refetching the server values keeps the changes made by the user.
 *
 * @param namespace namespace that owns this service
 * @param service single Spring Boot application or microservice that belongs to a given namespace
 * @param profile profile for which the changeset state is being resolved
 */
export const useChangesetState = (namespace: Namespace, service: Service, profile: Profile) => {
  const [draft] = useChangesetStorage(profile.id);
  const catalog = useServiceCatalogQuery(namespace.slug, service.slug);
  const values = useQuery(getProfilePropertiesQuery(namespace, service, profile));

  const data = useMemo(() => {
    if (!catalog.data || !values.data) {
      return undefined;
    }

    const base = generateStubChangesetState(namespace, service, profile, catalog.data, values.data);
    return rebaseChangeset(base, draft);
  }, [namespace, service, profile, catalog.data, values.data, draft]);

  return {
    ...values,
    data,
    isLoading: values.isLoading || catalog.isLoading,
  };
};

export const useRenameChangeset = (state: ChangesetState) => {
  const [, setDraft] = useChangesetStorage(state.profile.id);

  return useMutation({
    mutationFn: (name?: string): Promise<ChangesetState> => {
      return Promise.resolve({ ...state, name: name || 'Changeset draft' });
    },
    onSuccess(result: ChangesetState) {
      setDraft(draft => ({ ...(draft ?? EMPTY_CHANGESET_STORAGE_STATE), name: result.name }));
    },
  });
};

export const useDiscardChangeset = (changeset: ChangesetState) => {
  const [, setDraft] = useChangesetStorage(changeset.profile.id);

  return useMutation({
    mutationFn: (state: ChangesetState): Promise<ChangesetState> => Promise.resolve(state),
    onSuccess() {
      setDraft(null);
    },
  });
};

export const useApplyChangeset = (state: ChangesetState) => {
  const client = useQueryClient();
  const [, setDraft] = useChangesetStorage(state.profile.id);

  return useMutation({
    mutationFn: async (changeset: ChangesetState): Promise<ChangesetState> => {
      const { namespace, service, profile } = changeset;

      await request.post(`api/namespaces/${namespace.slug}/services/${service.slug}/profiles/${profile.slug}/apply`, {
        json: generateApplyRequestFromChangeset(changeset),
      }).json<VaultRevisionInformation>();

      return changeset;
    },
    onSuccess: async (changeset: ChangesetState) => {
      await client.invalidateQueries({
        predicate: query => {
          const key = query.queryKey;
          if (key[0] === 'vault') {
            return key[1] === changeset.profile.id;
          }
          return false;
        },
      });

      setDraft(null);
    },
  });
};

export const useSubmitChangeset = (state: ChangesetState) => {
  const client = useQueryClient();
  const [, setDraft] = useChangesetStorage(state.profile.id);

  return useMutation({
    mutationFn: async (changeset: ChangesetState): Promise<ChangeRequest> => {
      const { namespace, service, profile } = changeset;

      return request.post(`api/namespaces/${namespace.slug}/services/${service.slug}/profiles/${profile.slug}/submit`, {
        json: generateApplyRequestFromChangeset(changeset),
      }).json<ChangeRequest>();
    },
    onSuccess: async (changeRequest: ChangeRequest) => {
      await client.invalidateQueries({
        predicate: query => {
          const key = query.queryKey;
          if (key[0] === 'vault') {
            return key[1] === changeRequest.profile.id;
          }
          return false;
        },
      });

      setDraft(null);
    },
  });
};

const generatePropertyOperationMutation = (
  mutation: (draft: ChangesetStorageState | null | undefined, property: PropertyDescriptor, value?: ConfigurationPropertyValue<any>) => ChangesetStorageState | null | undefined,
) => (state: ChangesetState) => {
  const [, setDraft] = useChangesetStorage(state.profile.id);

  return useMutation({
    mutationFn: ({ property, value }: { property: PropertyDescriptor, value?: ConfigurationPropertyValue<any> }): Promise<void> => {
      setDraft(draft => mutation(draft, property, value));
      return Promise.resolve();
    },
  });
};

export const useAddProperty = generatePropertyOperationMutation(
  (draft, property, value) => upsertChangesetProperty(draft, {
    ...property, value, state: ConfigurationPropertyState.ADDED,
  }),
);

export const useImportProperties = (changeset: ChangesetState) => {
  const [, setDraft] = useChangesetStorage(changeset.profile.id);

  return useMutation({
    mutationFn: (properties: Array<ConfigurationProperty<any>>): Promise<void> => {
      setDraft(draft => properties.reduce((state, property) => upsertChangesetProperty(state, {
        ...property, state: ConfigurationPropertyState.UPDATED,
      }), draft ?? EMPTY_CHANGESET_STORAGE_STATE));
      return Promise.resolve();
    },
  });
};

export const useRestoreProperty = generatePropertyOperationMutation(
  (draft, property) => {
    // only removed properties can be restored, restoring them discards the change
    const change = draft?.properties.find(it => it.name === property.name);

    if (!draft || change?.state !== ConfigurationPropertyState.REMOVED) {
      return draft;
    }

    return { ...draft, properties: draft.properties.filter(it => it.name !== property.name) };
  },
);

export const useModifyProperty = generatePropertyOperationMutation(
  (draft, property, value) => upsertChangesetProperty(draft, {
    ...property, value, state: ConfigurationPropertyState.UPDATED,
  }),
);

export const useRemoveProperty = generatePropertyOperationMutation(
  // properties that do not exist on the server are dropped when the changeset is rebased
  (draft, property) => upsertChangesetProperty(draft, {
    ...property, state: ConfigurationPropertyState.REMOVED,
  }),
);

export const getChangeHistoryQuery = (namespace: Namespace, service: Service, profile: Profile, query?: ChangeHistoryQuery) => {
  return queryOptions({
    queryKey: vaultKeys.getChangeHistory(profile, query),
    placeholderData: keepPreviousData,
    queryFn: async ({ signal }): Promise<CursorResponse<ChangeHistory>> => {
      return await request.get(`api/namespaces/${namespace.slug}/services/${service.slug}/profiles/${profile.slug}/history`, {
        searchParams: query, signal,
      }).json<CursorResponse<ChangeHistory>>();
    },
  });
};

export const useGetChangeHistory = (namespace: Namespace, service: Service, profile: Profile, query?: ChangeHistoryQuery) => {
  return useQuery(getChangeHistoryQuery(namespace, service, profile, query));
};

export const getChangeHistoryDetailsQuery = (namespace: Namespace, service: Service, profile: Profile, history: ChangeHistory) => {
  return queryOptions({
    queryKey: vaultKeys.getChangeHistoryDetails(profile, history),
    queryFn: async () => {
      const { data } = await request.get(`api/namespaces/${namespace.slug}/services/${service.slug}/profiles/${profile.slug}/history/${history.revision}`)
        .json<CollectionResponse<ChangeHistoryRecord>>();

      return data;
    },
  });
};

export const useGetChangeHistoryDetails = (namespace: Namespace, service: Service, profile: Profile, history: ChangeHistory) => {
  return useQuery(getChangeHistoryDetailsQuery(namespace, service, profile, history));
};
