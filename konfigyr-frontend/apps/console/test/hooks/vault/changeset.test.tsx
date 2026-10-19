import { afterEach, describe, expect, test } from 'vitest';
import { QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import { createTestQueryClient } from '@konfigyr/test/helpers/query-client';
import { namespaces, profiles, services } from '@konfigyr/test/helpers/mocks';
import { server } from '@konfigyr/test/helpers/server';
import { rebaseChangeset, useChangesetState, useModifyProperty } from '@konfigyr/hooks/vault/changeset';
import { clearChangesetStorage, useChangesetStorage } from '@konfigyr/hooks/vault/changeset-storage';
import { ConfigurationPropertyState } from '@konfigyr/hooks/vault/types';

import type { ReactNode } from 'react';
import type { ChangesetState, ConfigurationProperty } from '@konfigyr/hooks/vault/types';

const PROPERTIES_URL = 'http://localhost/api/namespaces/:namespace/services/:service/profiles/:profile/properties';

const property = (
  name: string,
  encoded: string,
  state: ConfigurationPropertyState = ConfigurationPropertyState.UNCHANGED,
): ConfigurationProperty<string> => ({
  name,
  typeName: 'java.lang.String',
  schema: { type: 'string' },
  value: { encoded, decoded: encoded },
  state,
});

const changeset = (...properties: Array<ConfigurationProperty<any>>): ChangesetState => ({
  namespace: namespaces.konfigyr,
  service: services.konfigyrApi,
  profile: profiles.development,
  name: 'Changeset draft',
  state: 'DRAFT',
  properties,
  added: 0,
  modified: 0,
  deleted: 0,
});

const stateOf = (state: ChangesetState) => Object.fromEntries(
  state.properties.map(it => [it.name, [it.state, it.value?.encoded]]),
);

const createWrapper = () => {
  const client = createTestQueryClient();

  return ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  );
};

describe('hooks | vault | changeset', () => {
  afterEach(() => clearChangesetStorage());

  test('should return the base changeset when there is no draft', () => {
    const base = changeset(property('server.port', '8080'));

    expect(rebaseChangeset(base, null)).toBe(base);
  });

  test('should rebase the draft changes on top of the server values', () => {
    const base = changeset(
      property('server.port', '8080'),
      property('server.address', 'localhost'),
      property('spring.application.name', 'konfigyr'),
      property('spring.profiles.active', 'dev'),
    );

    const result = rebaseChangeset(base, {
      name: 'Updated draft',
      description: 'Draft description',
      properties: [
        property('server.port', '9090', ConfigurationPropertyState.UPDATED),
        property('server.address', 'localhost', ConfigurationPropertyState.REMOVED),
        property('spring.application.name', 'stale-value', ConfigurationPropertyState.UNCHANGED),
        property('logging.level.root', 'debug', ConfigurationPropertyState.ADDED),
      ],
    });

    expect(stateOf(result)).toStrictEqual({
      'server.port': [ConfigurationPropertyState.UPDATED, '9090'],
      'server.address': [ConfigurationPropertyState.REMOVED, 'localhost'],
      'spring.application.name': [ConfigurationPropertyState.UNCHANGED, 'konfigyr'],
      'spring.profiles.active': [ConfigurationPropertyState.UNCHANGED, 'dev'],
      'logging.level.root': [ConfigurationPropertyState.ADDED, 'debug'],
    });

    expect(result).toMatchObject({
      name: 'Updated draft',
      description: 'Draft description',
      added: 1,
      modified: 1,
      deleted: 1,
    });
  });

  test('should mark added property as updated when it exists on the server', () => {
    const result = rebaseChangeset(changeset(property('server.port', '8080')), {
      name: 'Changeset draft',
      properties: [property('server.port', '9090', ConfigurationPropertyState.ADDED)],
    });

    expect(stateOf(result)).toStrictEqual({
      'server.port': [ConfigurationPropertyState.UPDATED, '9090'],
    });
  });

  test('should mark updated property as added when it no longer exists on the server', () => {
    const result = rebaseChangeset(changeset(), {
      name: 'Changeset draft',
      properties: [property('server.port', '9090', ConfigurationPropertyState.UPDATED)],
    });

    expect(stateOf(result)).toStrictEqual({
      'server.port': [ConfigurationPropertyState.ADDED, '9090'],
    });
  });

  test('should drop removed property when it no longer exists on the server', () => {
    const result = rebaseChangeset(changeset(), {
      name: 'Changeset draft',
      properties: [property('server.port', '8080', ConfigurationPropertyState.REMOVED)],
    });

    expect(result.properties).toStrictEqual([]);
    expect(result.deleted).toBe(0);
  });

  test('should mark changed property as unchanged when its value matches the server value', () => {
    const result = rebaseChangeset(changeset(property('server.port', '9090')), {
      name: 'Changeset draft',
      properties: [property('server.port', '9090', ConfigurationPropertyState.UPDATED)],
    });

    expect(stateOf(result)).toStrictEqual({
      'server.port': [ConfigurationPropertyState.UNCHANGED, '9090'],
    });
    expect(result.modified).toBe(0);
  });

  test('should resolve the changeset state from the server values', async () => {
    const { result } = renderHook(
      () => useChangesetState(namespaces.konfigyr, services.konfigyrApi, profiles.development),
      { wrapper: createWrapper() },
    );

    await waitFor(() => {
      expect(result.current.data).toBeDefined();
    });

    expect(result.current.isLoading).toBe(false);
    expect(stateOf(result.current.data!)).toStrictEqual({
      'application.name': [ConfigurationPropertyState.UNCHANGED, 'konfigyr-frontend'],
      'application.profile': [ConfigurationPropertyState.UNCHANGED, 'staging'],
    });
  });

  test('should keep the draft changes when the server values are refetched', async () => {
    const { result } = renderHook(() => {
      const state = useChangesetState(namespaces.konfigyr, services.konfigyrApi, profiles.development);
      const [, setDraft] = useChangesetStorage(profiles.development.id);
      return { state, setDraft };
    }, { wrapper: createWrapper() });

    await waitFor(() => {
      expect(result.current.state.data).toBeDefined();
    });

    act(() => result.current.setDraft({
      name: 'Changeset draft',
      properties: [
        property('application.name', 'konfigyr-backend', ConfigurationPropertyState.UPDATED),
        property('server.port', '8080', ConfigurationPropertyState.ADDED),
      ],
    }));

    server.use(
      http.get(PROPERTIES_URL, () => HttpResponse.json({
        'application.name': 'konfigyr-frontend',
        'application.profile': 'production',
        'server.port': '9090',
      })),
    );

    await act(() => result.current.state.refetch());

    await waitFor(() => {
      expect(stateOf(result.current.state.data!)).toStrictEqual({
        'application.name': [ConfigurationPropertyState.UPDATED, 'konfigyr-backend'],
        'application.profile': [ConfigurationPropertyState.UNCHANGED, 'production'],
        'server.port': [ConfigurationPropertyState.UPDATED, '8080'],
      });
    });
  });

  test('should store property mutations in the changeset storage', async () => {
    const { result } = renderHook(() => {
      const state = useChangesetState(namespaces.konfigyr, services.konfigyrApi, profiles.development);
      return { state };
    }, { wrapper: createWrapper() });

    await waitFor(() => {
      expect(result.current.state.data).toBeDefined();
    });

    const { result: mutation } = renderHook(() => useModifyProperty(result.current.state.data!), { wrapper: createWrapper() });

    await act(() => mutation.current.mutateAsync({
      property: result.current.state.data!.properties[0],
      value: { encoded: 'konfigyr-backend', decoded: 'konfigyr-backend' },
    }));

    await waitFor(() => {
      expect(stateOf(result.current.state.data!)).toStrictEqual({
        'application.name': [ConfigurationPropertyState.UPDATED, 'konfigyr-backend'],
        'application.profile': [ConfigurationPropertyState.UNCHANGED, 'staging'],
      });
    });
  });
});
