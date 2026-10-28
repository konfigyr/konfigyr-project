import { act, renderHook, waitFor } from '@testing-library/react';
import { describe, expect, test } from 'vitest';
import { PropertyTransitionType } from '@konfigyr/hooks/vault/types';
import { usePropertiesImporter } from '@konfigyr/hooks/vault/properties-importer';

import type { ConfigurationProperty } from '@konfigyr/hooks/vault/types';

function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason?: unknown) => void;
  const promise = new Promise<T>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

const property = (name: string, value: string): ConfigurationProperty<string> => ({
  name,
  state: PropertyTransitionType.ADDED,
  value: { encoded: value, decoded: value },
  typeName: 'string',
  schema: { type: 'string' },
});

describe('hooks | vault | usePropertiesImporter', () => {
  test('should expose ready state by default', () => {
    const { result } = renderHook(() => usePropertiesImporter());

    expect(result.current.properties).toStrictEqual([]);
    expect(result.current.error).toBeUndefined();
    expect(result.current.isImporting).toBe(false);
    expect(result.current.isError).toBe(false);
    expect(result.current.isReady).toBe(true);
  });

  test('should store properties resolved by the import task', async () => {
    const { result } = renderHook(() => usePropertiesImporter());
    const task = deferred<Array<ConfigurationProperty<string>>>();

    let pending!: Promise<void>;
    act(() => {
      pending = result.current.importer(() => task.promise);
    });

    expect(result.current.isImporting).toBe(true);
    expect(result.current.isReady).toBe(false);

    await act(async () => {
      task.resolve([property('server.port', '8080')]);
      await pending;
    });

    expect(result.current.isImporting).toBe(false);
    expect(result.current.isReady).toBe(true);
    expect(result.current.properties).toStrictEqual([property('server.port', '8080')]);
  });

  test('should expose error and clear properties when the import task fails', async () => {
    const { result } = renderHook(() => usePropertiesImporter());

    await act(async () => {
      await result.current.importer(() => Promise.resolve([property('server.port', '8080')]));
    });
    expect(result.current.properties).toHaveLength(1);

    await act(async () => {
      await result.current.importer(() => Promise.reject(new Error('Unable to load properties')));
    });

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(result.current.error?.message).toBe('Unable to load properties');
    expect(result.current.properties).toStrictEqual([]);
    expect(result.current.isImporting).toBe(false);
    expect(result.current.isReady).toBe(false);
  });

  test('should wrap non-error rejections', async () => {
    const { result } = renderHook(() => usePropertiesImporter());

    await act(async () => {
      await result.current.importer(() => Promise.reject('failure'));
    });

    expect(result.current.error).toBeInstanceOf(Error);
    expect(result.current.error?.message).toBe('Import error');
  });

  test('should ignore results of superseded import tasks', async () => {
    const { result } = renderHook(() => usePropertiesImporter());
    const first = deferred<Array<ConfigurationProperty<string>>>();
    const second = deferred<Array<ConfigurationProperty<string>>>();

    let pendingFirst!: Promise<void>;
    let pendingSecond!: Promise<void>;
    act(() => {
      pendingFirst = result.current.importer(() => first.promise);
      pendingSecond = result.current.importer(() => second.promise);
    });

    await act(async () => {
      second.resolve([property('second', '2')]);
      await pendingSecond;
    });

    await act(async () => {
      first.resolve([property('first', '1')]);
      await pendingFirst;
    });

    expect(result.current.properties).toStrictEqual([property('second', '2')]);
    expect(result.current.isImporting).toBe(false);
  });

  test('should reset data and discard running import task', async () => {
    const { result } = renderHook(() => usePropertiesImporter());
    const task = deferred<Array<ConfigurationProperty<string>>>();

    let pending!: Promise<void>;
    act(() => {
      pending = result.current.importer(() => task.promise);
    });

    act(() => {
      result.current.reset();
    });

    await act(async () => {
      task.resolve([property('server.port', '8080')]);
      await pending;
    });

    expect(result.current.properties).toStrictEqual([]);
    expect(result.current.error).toBeUndefined();
    expect(result.current.isImporting).toBe(false);
    expect(result.current.isReady).toBe(true);
  });
});
