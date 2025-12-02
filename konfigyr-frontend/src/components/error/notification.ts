'use client';

import { useCallback} from 'react';
import { toast } from 'sonner';
import { createLogger } from '@konfigyr/logger';
import { normalizeError } from './normalize';

import type { ExternalToast } from 'sonner';

const logger = createLogger('components/error/notification');

export const useErrorNotification = (options?: ExternalToast) => {
  return useCallback((error: unknown): string | number => {
    logger.warn(error, `Rendering error notification for an error: ${error}`);

    const { title, detail: description } = normalizeError(error);
    return toast.error(title, { ...options, description });
  }, [options]);
};
