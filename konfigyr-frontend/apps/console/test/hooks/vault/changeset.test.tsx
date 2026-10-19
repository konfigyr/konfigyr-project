import { afterEach, describe, expect, test } from 'vitest';
import { QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import { createTestQueryClient } from '@konfigyr/test/helpers/query-client';
import { namespaces, profiles, services } from '@konfigyr/test/helpers/mocks';
import { server } from '@konfigyr/test/helpers/server';
import {
  rebaseChangeset,
  useAddProperty,
  useChangesetState,
  useDiscardChangeset,
  useModifyProperty,
  useRemoveProperty,
  useRestoreProperty,
} from '@konfigyr/hooks/vault/changeset';
import { clearChangesetStorage, useChangesetStorage } from '@konfigyr/hooks/vault/changeset-storage';
import { ConfigurationPropertyState } from '@konfigyr/hooks/vault/types';

import type { ReactNode } from 'react';
import type { ChangesetState, ConfigurationProperty } from '@konfigyr/hooks/vault/types';

const PROPERTIES_URL = 'http://localhost/api/namespaces/:namespace/services/:service/profiles/:profile/properties';
const CATALOG_URL = 'http://localhost/api/namespaces/:namespace/services/:service/catalog';

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
      expect(result.current.changeset).toBeDefined();
    });

    expect(result.current.isLoading).toBe(false);
    expect(stateOf(result.current.changeset!)).toStrictEqual({
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
      expect(result.current.state.changeset).toBeDefined();
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
      expect(stateOf(result.current.state.changeset!)).toStrictEqual({
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
      expect(result.current.state.changeset).toBeDefined();
    });

    const { result: mutation } = renderHook(() => useModifyProperty(result.current.state.changeset!), { wrapper: createWrapper() });

    await act(() => mutation.current.mutateAsync({
      property: result.current.state.changeset!.properties[0],
      value: { encoded: 'konfigyr-backend', decoded: 'konfigyr-backend' },
    }));

    await waitFor(() => {
      expect(stateOf(result.current.state.changeset!)).toStrictEqual({
        'application.name': [ConfigurationPropertyState.UPDATED, 'konfigyr-backend'],
        'application.profile': [ConfigurationPropertyState.UNCHANGED, 'staging'],
      });
    });
  });

  test('should apply consecutive property mutations started from the same render', async () => {
    const { result } = renderHook(() => {
      const state = useChangesetState(namespaces.konfigyr, services.konfigyrApi, profiles.development);
      return { state };
    }, { wrapper: createWrapper() });

    await waitFor(() => {
      expect(result.current.state.changeset).toBeDefined();
    });

    const current = result.current.state.changeset!;
    const { result: mutations } = renderHook(() => ({
      add: useAddProperty(current),
      modify: useModifyProperty(current),
      remove: useRemoveProperty(current),
    }), { wrapper: createWrapper() });

    const { add, modify, remove } = mutations.current;

    await act(async () => {
      await add.mutateAsync({
        property: property('server.port', '8080'),
        value: { encoded: '8080', decoded: '8080' },
      });
      await modify.mutateAsync({
        property: current.properties[0],
        value: { encoded: 'konfigyr-backend', decoded: 'konfigyr-backend' },
      });
      await remove.mutateAsync({ property: current.properties[1] });
    });

    await waitFor(() => {
      expect(stateOf(result.current.state.changeset!)).toStrictEqual({
        'application.name': [ConfigurationPropertyState.UPDATED, 'konfigyr-backend'],
        'application.profile': [ConfigurationPropertyState.REMOVED, 'staging'],
        'server.port': [ConfigurationPropertyState.ADDED, '8080'],
      });
    });
  });

  test('should discard added property when it is removed and restore removed property', async () => {
    const { result } = renderHook(() => {
      const state = useChangesetState(namespaces.konfigyr, services.konfigyrApi, profiles.development);
      return { state };
    }, { wrapper: createWrapper() });

    await waitFor(() => {
      expect(result.current.state.changeset).toBeDefined();
    });

    const current = result.current.state.changeset!;
    const { result: mutations } = renderHook(() => ({
      add: useAddProperty(current),
      remove: useRemoveProperty(current),
      restore: useRestoreProperty(current),
    }), { wrapper: createWrapper() });

    const { add, remove, restore } = mutations.current;

    await act(async () => {
      await add.mutateAsync({
        property: property('server.port', '8080'),
        value: { encoded: '8080', decoded: '8080' },
      });
      await remove.mutateAsync({ property: property('server.port', '8080') });
      await remove.mutateAsync({ property: current.properties[0] });
      await restore.mutateAsync({ property: current.properties[0] });
    });

    await waitFor(() => {
      expect(stateOf(result.current.state.changeset!)).toStrictEqual({
        'application.name': [ConfigurationPropertyState.UNCHANGED, 'konfigyr-frontend'],
        'application.profile': [ConfigurationPropertyState.UNCHANGED, 'staging'],
      });
    });
  });

  test('should report an error when the service catalog cannot be retrieved', async () => {
    server.use(
      http.get(CATALOG_URL, () => HttpResponse.json({ status: 500, title: 'Internal error' }, { status: 500 })),
    );

    const { result } = renderHook(
      () => useChangesetState(namespaces.konfigyr, services.konfigyrApi, profiles.development),
      { wrapper: createWrapper() },
    );

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.error).toBeDefined();
    expect(result.current.isLoading).toBe(false);
    expect(result.current.changeset).toBeUndefined();
    expect(result.current.catalog).toBeUndefined();
  });

  test('should not recreate unchanged properties when the draft changes', async () => {
    const { result } = renderHook(() => {
      const state = useChangesetState(namespaces.konfigyr, services.konfigyrApi, profiles.development);
      const [, setDraft] = useChangesetStorage(profiles.development.id);
      return { state, setDraft };
    }, { wrapper: createWrapper() });

    await waitFor(() => {
      expect(result.current.state.changeset).toBeDefined();
    });

    const unchanged = result.current.state.changeset!.properties[1];

    act(() => result.current.setDraft({
      name: 'Changeset draft',
      properties: [property('application.name', 'konfigyr-backend', ConfigurationPropertyState.UPDATED)],
    }));

    expect(result.current.state.changeset!.properties[0].value?.encoded).toBe('konfigyr-backend');
    expect(result.current.state.changeset!.properties[1]).toBe(unchanged);
  });

  test('should clear the draft and refetch the server values when changeset is discarded', async () => {
    const { result } = renderHook(() => {
      const state = useChangesetState(namespaces.konfigyr, services.konfigyrApi, profiles.development);
      const [, setDraft] = useChangesetStorage(profiles.development.id);
      const discard = useDiscardChangeset(state.changeset ?? ({ profile: profiles.development } as ChangesetState));
      return { state, setDraft, discard };
    }, { wrapper: createWrapper() });

    await waitFor(() => {
      expect(result.current.state.changeset).toBeDefined();
    });

    const current = result.current.state.changeset!;

    act(() => result.current.setDraft({
      name: 'Changeset draft',
      properties: [property('application.name', 'konfigyr-backend', ConfigurationPropertyState.UPDATED)],
    }));

    server.use(
      http.get(PROPERTIES_URL, () => HttpResponse.json({
        'application.name': 'konfigyr-frontend',
        'application.profile': 'production',
      })),
    );

    await act(() => result.current.discard.mutateAsync(current));

    expect(result.current.state.changeset!.name).toBe('Changeset draft');
    await waitFor(() => {
      expect(stateOf(result.current.state.changeset!)).toStrictEqual({
        'application.name': [ConfigurationPropertyState.UNCHANGED, 'konfigyr-frontend'],
        'application.profile': [ConfigurationPropertyState.UNCHANGED, 'production'],
      });
    });
  });
});
