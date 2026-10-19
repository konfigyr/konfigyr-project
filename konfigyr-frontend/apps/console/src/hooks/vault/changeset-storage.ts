import { useCallback, useSyncExternalStore } from 'react';

import type { Dispatch, SetStateAction } from 'react';
import type { ChangesetState } from '@konfigyr/hooks/vault/types';

export type ChangesetStorageState = Pick<ChangesetState, 'name' | 'description' | 'properties'>;

export type ChangesetStorage = [ChangesetStorageState | null, Dispatch<SetStateAction<ChangesetStorageState | undefined | null>>];

const store = new Map<string, ChangesetStorageState>();
const listeners = new Set<() => void>();

const notify = () => listeners.forEach(listener => listener());

const subscribe = (listener: () => void) => {
  listeners.add(listener);
  return () => listeners.delete(listener);
};

/**
 * Removes all changeset states from the in-memory store.
 */
export const clearChangesetStorage = () => {
  store.clear();
  notify();
};

export const useChangesetStorage = (key: string, initialValue?: ChangesetStorageState): ChangesetStorage => {
  const state = useSyncExternalStore(subscribe, () => store.get(key) ?? initialValue, () => initialValue);

  const setState = useCallback((action: SetStateAction<ChangesetStorageState | undefined | null>): void => {
    const current = store.get(key) ?? initialValue;
    const nextState = action instanceof Function ? action(current) : action;

    if (nextState === current) {
      return;
    }

    if (nextState === null || typeof nextState === 'undefined') {
      store.delete(key);
    } else {
      store.set(key, nextState);
    }

    notify();
  }, [key, initialValue]);

  return [state ?? null, setState];
};
