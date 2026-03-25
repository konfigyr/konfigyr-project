import { queryOptions, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getServiceCatalogQuery } from '@konfigyr/hooks/namespace/query';

import request from '@konfigyr/lib/http';
import { useJsonSchemeTransform } from '@konfigyr/hooks/artifactory/hooks';
import { Operation } from '@konfigyr/hooks/vault/types';

import type { PropertyDescriptor } from '@konfigyr/hooks/artifactory/types';
import type { Namespace, Service, ServiceCatalog } from '@konfigyr/hooks/namespace/types';
import type {
  ApplyRequest,
  ApplyResult,
  ChangeHistory,
  ChangeHistoryQuery,
  ChangesetState,
  ConfigurationProperty,
  ConfigurationPropertyValue,
  Profile,
  PropertyChange,
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
  getChangeHistory: (profile: Profile, query?: ChangeHistoryQuery) => ['vault', profile.id, 'history', query?.page, query?.size],
};

const generateApplyRequestFromChangeset = (payload: ChangesetState): ApplyRequest => {
  const changes = payload.properties.reduce((acc, property) => {
    const { name, value } = property;

    switch (property.state) {
      case 'added':
        return [...acc, { name, value: value?.encoded, operation: Operation.CREATE }];
      case 'modified':
        return [...acc, { name, value: value?.encoded, operation: Operation.MODIFY }];
      case 'deleted':
        return [...acc, { name, operation: Operation.REMOVE }];
      default:
        return acc;
    }
  }, [] as Array<PropertyChange>);

  return {
    name: payload.name,
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
      state: 'unchanged',
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
    added: properties.filter(it => it.state === 'added').length,
    modified: properties.filter(it => it.state === 'modified').length,
    deleted: properties.filter(it => it.state === 'deleted').length,
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
 * @param service single Spring Boot application or microservice that belongs to a given namesasce
 * @param profile profile for which the changeset state is being resolved
 */
export const useChangesetState = (namespace: Namespace, service: Service, profile: Profile) => {
  return useQuery(getChangesetStateQuery(namespace, service, profile));
};

export const useRenameChangeset = (state: ChangesetState) => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: async (name?: string): Promise<ChangesetState> => {
      await new Promise(resolve => setTimeout(resolve, 300));
      return { ...state, name: name || 'Changeset draft' };
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
      }).json<ApplyResult>();
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
    mutationFn: async (state: ChangesetState): Promise<ChangesetState> => {
      await new Promise(resolve => setTimeout(resolve, 300));
      return client.fetchQuery(getChangesetStateQuery(state.namespace, state.service, state.profile));
    },
    onSuccess(state: ChangesetState) {
      client.setQueryData(vaultKeys.getChangeset(state.profile), state);
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

      const deleted = properties.filter(p => p.state === 'deleted').length;
      const modified = properties.filter(p => p.state === 'modified').length;
      const added = properties.filter(p => p.state === 'added').length;

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
      { ...property, value, state: 'added' },
    ];
  },
);

export const useRestoreProperty = generatePropertyOperationMutation(
  (state, property) => {
    const properties: Array<ConfigurationProperty<any>> = state.properties.map(it => {
      if (property.name === it.name && it.state === 'deleted') {
        return { ...it, state: 'unchanged' };
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
        return { ...it, value, state: it.state === 'added' ? 'added' : 'modified' };
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
        if (it.state === 'added') {
          return state;
        }
        // modified properties should be marked as deleted
        return [ ...state, { ...it, state: 'deleted' } ];
      }
      return [ ...state, it];
    }, [] as Array<ConfigurationProperty<any>>);

    return properties;
  },
);

export const getChangeHistory = (namespace: Namespace, service: Service, profile: Profile, query?: ChangeHistoryQuery) => {
  return queryOptions({
    queryKey: vaultKeys.getChangeHistory(profile, query),
    queryFn: async () => {
      return await request.get(`api/namespaces/${namespace.slug}/services/${service.slug}/profiles/${profile.slug}/history`, {
        searchParams: query,
      }).json<{
        data: Array<ChangeHistory>;
        metadata: {
          size: number,
          number: number,
          total: number,
          pages: number,
        }
      }>();
    },
  });
};

export const useGetChangeHistory = (namespace: Namespace, service: Service, profile: Profile, query?: ChangeHistoryQuery) => {
  return useQuery(getChangeHistory(namespace, service, profile, query));
};


