'use client';

import { useCallback } from 'react';
import { toast } from '@konfigyr/components/ui/toast';
import { createLogger } from '@konfigyr/logger';
import { normalizeError } from './normalize';

import type { ToastManagerAddOptions } from '@konfigyr/components/ui/toast';

const logger = createLogger('components/error/notification');

export const useErrorNotification = (options?: ToastManagerAddOptions<any>) => {
  return useCallback((error: unknown): string | number => {
    logger.warn(error, `Rendering error notification for an error: ${error}`);

    const { title, detail: description } = normalizeError(error);
    return toast.add({ ...options, type: 'error', title, description });
  }, [options]);
};
