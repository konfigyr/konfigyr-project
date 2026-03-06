import {queryOptions, useMutation, useQuery, useQueryClient} from '@tanstack/react-query';

import {
  Operation,
} from '@konfigyr/hooks/vault/types';
import request from '@konfigyr/lib/http';
import type {
  ApplyRequest,
  ApplyResult,
  ChangesetState,
  ConfigurationProperty,
  Profile,
  PropertyChange,
  PropertyDescriptor} from '@konfigyr/hooks/vault/types';
import type { Namespace, Service } from '@konfigyr/hooks/namespace/types';
/**
 * Keys used to store the Changeset states in the query client.
 */
export const vaultKeys = {
  getChangeset: (profile: Profile) => ['vault', profile.id, 'changeset'],
};

const generateCommitChangesetMutation = (payload: ChangesetState) : ApplyRequest => {
  const changes = payload.properties.reduce(
    (acc, property) => {
      const { name, value } = property;
      switch (property.state) {
        case 'modified':
          return [...acc, { name, value, operation: Operation.MODIFY}];
        case 'deleted':
          return [...acc, { name, value, operation: Operation.REMOVE}];
        case 'added':
          return [...acc, { name, value, operation: Operation.CREATE}];
        case 'unchanged':
        default:
          return acc;
      }
    }, [] as Array<PropertyChange>);

  return {
    name: payload.name,
    changes,
  };
};

const generateStubChangesetState = (namespace: Namespace, service: Service, profile: Profile, properties: Array<ConfigurationProperty>): ChangesetState => ({
  namespace,
  service,
  profile,
  name: 'Changeset draft',
  state: 'DRAFT',
  properties: properties,
  added: properties.filter(it => it.state === 'added').length,
  modified: properties.filter(it => it.state === 'modified').length,
  deleted: properties.filter(it => it.state === 'deleted').length,
});

/**
 * Attempts to resolve existing or create a new changeset state for the given profile and current user account.
 *
 * @param namespace namespace that owns this service
 * @param service single Spring Boot application or microservice that belongs to a given namesasce
 * @param profile profile for which the changeset state is being resolved
 * @returns TansStack query options to retrieve the keysets
 */
export const getChangesetStateQuery = (namespace: Namespace, service: Service, profile: Profile) => {
  return queryOptions({
    queryKey: vaultKeys.getChangeset(profile),
    queryFn: async () => {
      const response = await request.get(`api/namespaces/${namespace.slug}/services/${service.slug}/profiles/${profile.name}/properties`).json<{ data: Array<ConfigurationProperty> }>();
      return generateStubChangesetState(namespace, service, profile, response.data);
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
    mutationFn: async (state: ChangesetState): Promise<ChangesetState> => {
      const {namespace, service, profile} = state;
      return client.fetchQuery(getChangesetStateQuery(namespace, service, profile));
    },
  });
};

export const useApplyChangeset = () => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: async (payload: ChangesetState): Promise<ChangesetState> => {
      const { namespace, service, profile } = payload;
      const req = generateCommitChangesetMutation(payload);

      await request.post(`api/namespaces/${namespace.slug}/services/${service.slug}/profiles/${profile.slug}/apply`, {
        json: req,
      }).json<ApplyResult>();

      return client.fetchQuery(getChangesetStateQuery(namespace, service, profile));
    },
  });
};

export const useSubmitChangeset = () => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: async (state: ChangesetState): Promise<ChangesetState> => {
      await new Promise(resolve => setTimeout(resolve, 300));
      return generateStubChangesetState(state.namespace, state.service, state.profile, []);
    },
    onSuccess(state: ChangesetState) {
      client.setQueryData(vaultKeys.getChangeset(state.profile), state);
    },
  });
};

const generatePropertyOperationMutation = (
  mutation: (state: ChangesetState, property: PropertyDescriptor, value?: string) => Array<ConfigurationProperty>,
) => (state: ChangesetState) => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: async ({ property, value }: { property: PropertyDescriptor, value?: string }): Promise<ChangesetState> => {
      await new Promise(resolve => setTimeout(resolve, 300));

      const properties = mutation(state, property, value);

      const deleted = properties.filter(p => p.state === 'deleted').length;
      const modified = properties.filter(p => p.state === 'modified').length;
      const added = properties.filter(p => p.state === 'added').length;

      return { ...state, properties, deleted, modified, added };
    },
    onSuccess(result: ChangesetState) {
      client.setQueryData(vaultKeys.getChangeset(state.profile), result);
    },
  });
};

export const useAddProperty = generatePropertyOperationMutation(
  (state, property, value) => {
    return [
      { ...property, value, state: 'added' },
      ...state.properties,
    ];
  },
);

export const useRestoreProperty = generatePropertyOperationMutation(
  (state, property) => {
    const properties: Array<ConfigurationProperty> = state.properties.map(it => {
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
    const properties: Array<ConfigurationProperty> = state.properties.map(it => {
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
    const properties: Array<ConfigurationProperty> = changeset.properties.reduce((state, it) => {
      if (property.name === it.name) {
        // if the property was added, it should be removed from the changeset as well
        if (it.state === 'added') {
          return state;
        }
        // modified properties should be marked as deleted
        return [ ...state, { ...it, state: 'deleted' } ];
      }
      return [ ...state, it];
    }, [] as Array<ConfigurationProperty>);

    return properties;
  },
);
