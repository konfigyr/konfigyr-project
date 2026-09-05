import { HTTPError } from 'ky';
import { useMemo } from 'react';
import {
  GeneralErrorDetail,
  GeneralErrorTitle,
} from '@konfigyr/components/messages/globals';

import type { ReactNode } from 'react';

export interface NormalizedError {
  title: string | ReactNode,
  detail: string | ReactNode;
}

export const useNormalizeError = (error?: any): NormalizedError | null => useMemo(
  () => {
    if (error === undefined || error === null) {
      return null;
    }
    return normalizeError(error);
  }, [error],
);

export const normalizeError = (error: any): NormalizedError => {
  let title, detail;

  if (error instanceof HTTPError) {
    title = error.problem?.title;
    detail = error.problem?.detail;
  } else if ('title' in error && 'detail' in error) {
    title = error.title;
    detail = error.detail;
  }

  if (!title) {
    title = <GeneralErrorTitle />;
  }

  if (!detail) {
    detail = <GeneralErrorDetail />;
  }

  return { title, detail };
};
