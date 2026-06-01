import { useCallback } from 'react';
import clipboard from 'copy-to-clipboard';

export interface UseClipboardOptions {
  format?: 'text/plain' | 'text/html';
}

export const useClipboard = ({ format = 'text/plain' }: UseClipboardOptions = {}): (text: string) => Promise<boolean> => {
  return useCallback(async (text: string) => {
    // Omit the `format` configuration property for `text/plain` so `copy-to-clipboard`
    // uses the simple `navigator.clipboard.writeText` path instead of the ClipboardItem path.
    return clipboard(text, { format: format !== 'text/plain' ? format : undefined, debug: true });
  }, [format]);
};
