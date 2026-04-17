import { keepPreviousData, queryOptions, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getServiceCatalogQuery } from '@konfigyr/hooks/namespace/query';

import request from '@konfigyr/lib/http';
import { useJsonSchemeTransform } from '@konfigyr/hooks/artifactory/hooks';
import { ConfigurationPropertyState, Operation } from '@konfigyr/hooks/vault/types';

import type { PropertyDescriptor } from '@konfigyr/hooks/artifactory/types';
import type { CollectionResponse, CursorResponse } from '@konfigyr/hooks/hateoas/types';
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
    const transform = useJsonSchemeTransform<any>(descriptor?.schema ?? DEFAULT_PROPERTY_DESCRIPTOR.schema);
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
    added: properties.filter(it => it.state === ConfigurationPropertyState.ADDED).length,
    modified: properties.filter(it => it.state === ConfigurationPropertyState.UPDATED).length,
    deleted: properties.filter(it => it.state === ConfigurationPropertyState.REMOVED).length,
  };
};

/**
 * Attempts to resolve existing or create a new changeset state for the given profile and current user account.
 *
 * @param namespace namespace that owns this service
 * @param service single Spring Boot application or microservice that belongs to a given namesasce
 * @param profile profile for which the changeset state is being resolved
 * @returns TansStack query options to retrieve the keysets
 */
export const getChangesetStateQuery = (namespace: Namespace, service: Service, profile: Profile) => {
  const queryClient = useQueryClient();

  return queryOptions({
    queryKey: vaultKeys.getChangeset(profile),
    queryFn: async (): Promise<ChangesetState> => {
      const catalog = await queryClient.ensureQueryData(getServiceCatalogQuery(namespace.slug, service.slug));

      const response = await request.get(`api/namespaces/${namespace.slug}/services/${service.slug}/profiles/${profile.slug}/properties`)
        .json<Record<string, string>>();

      return generateStubChangesetState(namespace, service, profile, catalog, response);
    },
  });
};

/**
 * Hook that retrieves the current changeset state for the given profile and current user account.
 *
 * @param namespace namespace that owns this service
 * @param service single Spring Boot application or microservice that belongs to a given namespace
 * @param profile profile for which the changeset state is being resolved
 */
export const useChangesetState = (namespace: Namespace, service: Service, profile: Profile) => {
  return useQuery(getChangesetStateQuery(namespace, service, profile));
};

export const useRenameChangeset = (state: ChangesetState) => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: (name?: string): Promise<ChangesetState> => {
      return Promise.resolve({ ...state, name: name || 'Changeset draft' });
    },
    onSuccess(result: ChangesetState) {
      client.setQueryData(vaultKeys.getChangeset(state.profile), result);
    },
  });
};

export const useDiscardChangeset = () => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: (state: ChangesetState): Promise<ChangesetState> => Promise.resolve(state),
    onSuccess: async (state: ChangesetState)=> {
      await client.invalidateQueries({
        queryKey: vaultKeys.getChangeset(state.profile),
      });
    },
  });
};

export const useApplyChangeset = () => {
  const client = useQueryClient();

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
    },
  });
};

export const useSubmitChangeset = () => {
  const client = useQueryClient();

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
    },
  });
};

const generatePropertyOperationMutation = (
  mutation: (state: ChangesetState, property: PropertyDescriptor, value?: ConfigurationPropertyValue<any>) => Array<ConfigurationProperty<any>>,
) => (state: ChangesetState) => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: async ({ property, value }: { property: PropertyDescriptor, value?: ConfigurationPropertyValue<any> }): Promise<ChangesetState> => {
      const properties = mutation(state, property, value);

      const deleted = properties.filter(p => p.state === ConfigurationPropertyState.REMOVED).length;
      const modified = properties.filter(p => p.state === ConfigurationPropertyState.UPDATED).length;
      const added = properties.filter(p => p.state === ConfigurationPropertyState.ADDED).length;

      return Promise.resolve({ ...state, properties, deleted, modified, added });
    },
    onSuccess(result: ChangesetState) {
      client.setQueryData(vaultKeys.getChangeset(state.profile), result);
    },
  });
};

export const useAddProperty = generatePropertyOperationMutation(
  (state, property, value) => {
    return [
      ...state.properties,
      { ...property, value, state: ConfigurationPropertyState.ADDED },
    ];
  },
);

export const useRestoreProperty = generatePropertyOperationMutation(
  (state, property) => {
    const properties: Array<ConfigurationProperty<any>> = state.properties.map(it => {
      if (property.name === it.name && it.state === ConfigurationPropertyState.REMOVED) {
        return { ...it, state: ConfigurationPropertyState.UNCHANGED };
      }
      return it;
    });

    return properties;
  },
);

export const useModifyProperty = generatePropertyOperationMutation(
  (state, property, value) => {
    const properties: Array<ConfigurationProperty<any>> = state.properties.map(it => {
      if (property.name === it.name) {
        return {
          ...it,
          value,
          state: it.state === ConfigurationPropertyState.ADDED
            ? ConfigurationPropertyState.ADDED : ConfigurationPropertyState.UPDATED,
        };
      }
      return it;
    });

    return properties;
  },
);

export const useRemoveProperty = generatePropertyOperationMutation(
  (changeset, property) => {
    const properties: Array<ConfigurationProperty<any>> = changeset.properties.reduce((state, it) => {
      if (property.name === it.name) {
        // if the property was added, it should be removed from the changeset as well
        if (it.state === ConfigurationPropertyState.ADDED) {
          return state;
        }
        // modified properties should be marked as deleted
        return [ ...state, { ...it, state: ConfigurationPropertyState.REMOVED } ];
      }
      return [ ...state, it];
    }, [] as Array<ConfigurationProperty<any>>);

    return properties;
  },
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
