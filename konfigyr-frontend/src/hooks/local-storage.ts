import { useCallback, useEffect, useMemo, useSyncExternalStore } from 'react';
import { createLogger } from '@konfigyr/logger';

import type { Dispatch, SetStateAction } from 'react';

const STORAGE_EVENT_TYPE = 'storage';
const logger = createLogger('hooks/local-storage');

function dispatchStorageEvent(key: string, newValue?: string | null) {
  window.dispatchEvent(new StorageEvent(STORAGE_EVENT_TYPE, { key, newValue }));
}

const setLocalStorageItem = (key: string, value?: unknown) => {
  const serialized = JSON.stringify(value);
  window.localStorage.setItem(key, serialized);
  dispatchStorageEvent(key, serialized);
};

const removeLocalStorageItem = (key: string) => {
  window.localStorage.removeItem(key);
  dispatchStorageEvent(key, null);
};

const getLocalStorageItem = (key: string) => {
  return window.localStorage.getItem(key);
};

const useLocalStorageSubscribe = (callback: (event: StorageEvent) => void) => {
  window.addEventListener(STORAGE_EVENT_TYPE, callback);
  return () => window.removeEventListener(STORAGE_EVENT_TYPE, callback);
};

export type LocalStorageState<T> = [T | null, Dispatch<SetStateAction<T | undefined | null>>];

export function useLocalStorage<T>(key: string, initialValue?: T): LocalStorageState<T> {
  const store = useSyncExternalStore(useLocalStorageSubscribe, () => getLocalStorageItem(key));

  const value = useMemo(() => {
    let parsed;

    try {
      parsed = store ? JSON.parse(store) : initialValue;
    } catch (error) {
      logger.error(error, `Failed to process local storage value of '${store}', falling back to: ${initialValue}`);
      parsed = initialValue;
    }

    return parsed ?? null;
  }, [store, initialValue]);

  const setState = useCallback((action: SetStateAction<T | undefined | null>): void => {
    let nextState;

    if (action instanceof Function) {
      nextState = action(value);
    } else {
      nextState = action;
    }

    if (nextState === value) {
      return;
    }

    if (nextState === null || typeof nextState === 'undefined') {
      removeLocalStorageItem(key);
    } else {
      setLocalStorageItem(key, nextState);
    }
  }, [key, value]);

  useEffect(() => {
    if (getLocalStorageItem(key) === null && typeof initialValue !== 'undefined') {
      setLocalStorageItem(key, initialValue);
    }
  }, [key, initialValue]);

  return [value, setState];
}
