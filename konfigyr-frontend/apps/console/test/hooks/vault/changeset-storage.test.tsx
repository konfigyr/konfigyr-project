import { afterEach, describe, expect, test } from 'vitest';
import { renderToString } from 'react-dom/server';
import { act, renderHook } from '@testing-library/react';
import { clearChangesetStorage, useChangesetStorage } from '@konfigyr/hooks/vault/changeset-storage';
import { ConfigurationPropertyState } from '@konfigyr/hooks/vault/types';

import type { ChangesetStorageState } from '@konfigyr/hooks/vault/changeset-storage';
import type { ConfigurationProperty } from '@konfigyr/hooks/vault/types';

const property = (name: string, encoded: string): ConfigurationProperty<string> => ({
  name,
  typeName: 'java.lang.String',
  schema: { type: 'string' },
  value: { encoded, decoded: encoded },
  state: ConfigurationPropertyState.ADDED,
});

const initialValue: ChangesetStorageState = { name: 'Changeset draft', properties: [] };

describe('hooks | vault | changeset-storage', () => {
  afterEach(() => clearChangesetStorage());

  test('should return null when there is no stored state or initial value', () => {
    const { result } = renderHook(() => useChangesetStorage('profile-id'));

    expect(result.current[0]).toBeNull();
  });

  test('should return the initial value when there is no stored state', () => {
    const { result } = renderHook(() => useChangesetStorage('profile-id', initialValue));

    expect(result.current[0]).toBe(initialValue);
  });

  test('should store the state and reset it to the initial value', () => {
    const { result } = renderHook(() => useChangesetStorage('profile-id', initialValue));
    const state = { name: 'Updated draft', properties: [property('server.port', '8080')] };

    act(() => result.current[1](state));
    expect(result.current[0]).toBe(state);

    act(() => result.current[1](null));
    expect(result.current[0]).toBe(initialValue);

    act(() => result.current[1](state));
    act(() => result.current[1](undefined));
    expect(result.current[0]).toBe(initialValue);
  });

  test('should compute the state using the update function', () => {
    const { result } = renderHook(() => useChangesetStorage('profile-id', initialValue));

    act(() => result.current[1](previous => ({ ...previous!, name: `${previous!.name} - computed` })));

    expect(result.current[0]).toStrictEqual({ name: 'Changeset draft - computed', properties: [] });
  });

  test('should apply consecutive updates on top of each other', () => {
    const { result } = renderHook(() => useChangesetStorage('profile-id', initialValue));
    const [, setState] = result.current;

    act(() => {
      setState(previous => ({ ...previous!, properties: [...previous!.properties, property('server.port', '8080')] }));
      setState(previous => ({ ...previous!, properties: [...previous!.properties, property('server.address', 'localhost')] }));
    });

    expect(result.current[0]?.properties.map(it => it.name)).toStrictEqual(['server.port', 'server.address']);
  });

  test('should keep the same dispatch function when state changes', () => {
    const { result } = renderHook(() => useChangesetStorage('profile-id', initialValue));
    const [, setState] = result.current;

    act(() => setState({ ...initialValue, name: 'Updated draft' }));

    expect(result.current[1]).toBe(setState);
  });

  test('should not re-render when the next state is the same as the current one', () => {
    let renders = 0;
    const { result } = renderHook(() => {
      renders++;
      return useChangesetStorage('profile-id', initialValue);
    });

    act(() => result.current[1](initialValue));
    act(() => result.current[1](previous => previous));

    expect(renders).toBe(1);
  });

  test('should share the state between hooks using the same key', () => {
    const { result: first } = renderHook(() => useChangesetStorage('profile-id'));
    const { result: second } = renderHook(() => useChangesetStorage('profile-id'));

    act(() => first.current[1]({ ...initialValue, name: 'Shared draft' }));

    expect(second.current[0]?.name).toBe('Shared draft');
  });

  test('should isolate the state between different keys', () => {
    const { result: first } = renderHook(() => useChangesetStorage('first-profile'));
    const { result: second } = renderHook(() => useChangesetStorage('second-profile'));

    act(() => first.current[1]({ ...initialValue, name: 'First draft' }));

    expect(first.current[0]?.name).toBe('First draft');
    expect(second.current[0]).toBeNull();
  });

  test('should keep the state after the hook is unmounted', () => {
    const { result, unmount } = renderHook(() => useChangesetStorage('profile-id'));

    act(() => result.current[1]({ ...initialValue, name: 'Persisted draft' }));
    unmount();

    const { result: remounted } = renderHook(() => useChangesetStorage('profile-id'));
    expect(remounted.current[0]?.name).toBe('Persisted draft');
  });

  test('should clear all stored states and notify the hooks', () => {
    const { result: first } = renderHook(() => useChangesetStorage('first-profile', initialValue));
    const { result: second } = renderHook(() => useChangesetStorage('second-profile'));

    act(() => {
      first.current[1]({ ...initialValue, name: 'First draft' });
      second.current[1]({ ...initialValue, name: 'Second draft' });
    });

    act(() => clearChangesetStorage());

    expect(first.current[0]).toBe(initialValue);
    expect(second.current[0]).toBeNull();
  });

  test('should render the initial value on the server', () => {
    const Component = () => {
      const [state] = useChangesetStorage('profile-id', initialValue);
      return <span>{state?.name}</span>;
    };

    expect(renderToString(<Component />)).toBe('<span>Changeset draft</span>');
  });
});
