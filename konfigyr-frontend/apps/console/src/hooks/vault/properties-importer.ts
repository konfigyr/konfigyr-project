import { useCallback, useRef, useState } from 'react';

import type { ConfigurationProperty } from '@konfigyr/hooks/vault/types';

type ImportedProperties = Array<ConfigurationProperty<string>>;

/**
 * Function that runs an import task and stores the properties that it resolves.
 *
 * Only the result of the most recently started task is kept. Results of tasks that were
 * superseded by a newer task, or by a reset, are discarded.
 */
export type PropertiesImporter = (task: () => Promise<ImportedProperties>) => Promise<void>;

/**
 * React hook that keeps the state of the configuration properties that are being imported
 * into a profile, regardless of the source that they are imported from.
 *
 * @returns An object containing:
 * - `properties` - The properties resolved by the latest successful import task.
 * - `error` - The error thrown by the latest import task, if it failed.
 * - `importer` - Function that runs an import task and stores its result.
 * - `reset` - Function that clears the imported properties and errors, and discards any running task.
 * - `isImporting` - Indicates whether an import task is currently in progress.
 * - `isError` - Indicates whether the latest import task failed.
 * - `isReady` - Indicates idle state (not loading and no error).
 */
export function usePropertiesImporter () {
  const [properties, setProperties] = useState<ImportedProperties>([]);
  const [isImporting, setIsImporting] = useState(false);
  const [error, setError] = useState<Error>();
  const sequenceRef = useRef(0);

  const importer: PropertiesImporter = useCallback(async (task) => {
    const currentSequence = ++sequenceRef.current;
    setIsImporting(true);
    setError(undefined);

    try {
      const imported = await task();

      if (sequenceRef.current !== currentSequence) {
        return;
      }
      setProperties(imported);
      setIsImporting(false);
      setError(undefined);
    } catch (e) {
      if (sequenceRef.current !== currentSequence) {
        return;
      }
      setProperties([]);
      setIsImporting(false);
      setError(e instanceof Error ? e : new Error('Import error'));
    }
  }, []);

  const reset = useCallback(() => {
    sequenceRef.current++;
    setProperties([]);
    setError(undefined);
    setIsImporting(false);
  }, []);

  return {
    properties,
    error,
    importer,
    reset,
    isImporting,
    isError: !!error,
    isReady: !isImporting && !error,
  };
}
