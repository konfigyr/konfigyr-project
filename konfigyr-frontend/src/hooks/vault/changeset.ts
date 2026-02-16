import { queryOptions, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import type {
  ChangesetState,
  ConfigurationProperty,
  Profile,
  PropertyDescriptor,
} from '@konfigyr/hooks/vault/types';

const PROPERTIES: Array<ConfigurationProperty> = [{
  name: 'spring.datasource.url',
  description: 'JDBC URL of the database. Auto-detected based on the classpath if not set.',
  type: 'java.util.List<com.acme.AcmeProperties$EnumeratedOptions>',
  value: 'jdbc:postgresql://db.configvault.io:5432/prod',
  state: 'unchanged',
}, {
  name: 'spring.datasource.hikari.maximum-pool-size',
  description: 'Maximum number of connections that HikariCP will keep in the pool, including both idle and in-use connections.',
  type: 'java.lang.Integer',
  value: '20',
  state: 'modified',
}, {
  name: 'spring.datasource.password',
  description: 'Login password of the database. Set using a vault-backed secret for production deployments.',
  type: 'java.lang.String',
  value: 'vault:secret/db#password',
  state: 'unchanged',
}, {
  name: 'spring.http.encoding.charset',
  description: 'Charset of HTTP requests and responses. Added to the Content-Type header if not set explicitly. Deprecated since Spring Boot 2.3.',
  type: 'java.lang.String',
  value: 'UTF-8',
  deprecation: {
    reason: 'This property is deprecated since Spring Boot 2.3.',
  },
  state: 'deleted',
}, {
  name: 'spring.flyway.enabled',
  description: 'Whether to enable Flyway database migrations on startup.',
  type: 'java.lang.Boolean',
  value: 'true',
  deprecation: {
    reason: 'This property is deprecated since Spring Boot 2.3.',
  },
  state: 'unchanged',
}, {
  name: 'spring.kafka.bootstrap-servers',
  description: 'Comma-delimited list of host:port pairs for establishing the initial Kafka cluster connection.',
  type: 'java.lang.String',
  value: 'kafka-01.configvault.io:9092,kafka-02.configvault.io:9092',
  state: 'added',
}, {
  name: 'spring.jpa.hibernate.ddl-auto',
  description: 'DDL mode. Embedded databases default to \'create-drop\'. Be careful in production -- you probably want \'validate\' or \'none\'.',
  type: 'java.lang.String',
  value: 'validate',
  state: 'unchanged',
}, {
  name: 'spring.cache.type',
  description: 'Cache type. By default, auto-detected according to the environment. Force a specific cache type by setting this.',
  type: 'java.lang.String',
  value: 'redis',
  state: 'unchanged',
}, {
  name: 'logging.level.org.springframework.web',
  description: 'Log level for the Spring Web framework package. Set to DEBUG for request/response tracing.',
  type: 'java.lang.String',
  value: 'INFO',
  state: 'unchanged',
}];

const generateStubChangesetState = (profile: Profile): ChangesetState => ({
  profile,
  name: 'Changeset draft',
  state: 'DRAFT',
  properties: PROPERTIES,
  added: PROPERTIES.filter(it => it.state === 'added').length,
  modified: PROPERTIES.filter(it => it.state === 'modified').length,
  deleted: PROPERTIES.filter(it => it.state === 'deleted').length,
});

/**
 * Keys used to store the Changeset states in the query client.
 */
export const vaultKeys = {
  getChangeset: (profile: Profile) => ['vault', profile.id, 'changeset'],
};

/**
 * Attempts to resolve existing or create a new changeset state for the given profile and current user account.
 *
 * @param profile profile for which the changeset state is being resolved
 * @returns TansStack query options to retrieve the keysets
 */
export const getChangesetStateQuery = (profile: Profile) => {
  return queryOptions({
    queryKey: vaultKeys.getChangeset(profile),
    // TODO: resolve the state from the backend
    queryFn: (): ChangesetState => generateStubChangesetState(profile),
    staleTime: Infinity,
  });
};

/**
 * Hook that retrieves the current changeset state for the given profile and current user account.
 *
 * @param profile profile for which the changeset state is being resolved
 */
export const useChangesetState = (profile: Profile) => {
  return useQuery(getChangesetStateQuery(profile));
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
      await new Promise(resolve => setTimeout(resolve, 300));
      return generateStubChangesetState(state.profile);
    },
    onSuccess(state: ChangesetState) {
      client.setQueryData(vaultKeys.getChangeset(state.profile), state);
    },
  });
};

export const useCommitChangeset = () => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: async (state: ChangesetState): Promise<ChangesetState> => {
      await new Promise(resolve => setTimeout(resolve, 300));
      return generateStubChangesetState(state.profile);
    },
    onSuccess(state: ChangesetState) {
      client.setQueryData(vaultKeys.getChangeset(state.profile), state);
    },
  });
};

const generatePropertyOperationMutation = (
  mutation: (state: ChangesetState, property: PropertyDescriptor, value?: string) => ChangesetState,
) => (state: ChangesetState) => {
  const client = useQueryClient();

  return useMutation({
    mutationFn: async ({ property, value }: { property: PropertyDescriptor, value?: string }): Promise<ChangesetState> => {
      await new Promise(resolve => setTimeout(resolve, 300));
      return mutation(state, property, value);
    },
    onSuccess(result: ChangesetState) {
      client.setQueryData(vaultKeys.getChangeset(state.profile), result);
    },
  });
};

export const useAddProperty = generatePropertyOperationMutation(
  (state, property, value) => {
    const properties: Array<ConfigurationProperty> = [
      { ...property, value, state: 'added' },
      ...state.properties,
    ];

    return { ...state, properties, added: properties.length };
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

    return { ...state, properties, modified: properties.length };
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

    return { ...state, properties, modified: properties.length };
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

    return { ...changeset, properties, deleted: properties.length };
  },
);
