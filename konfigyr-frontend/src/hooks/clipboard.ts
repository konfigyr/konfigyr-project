import { useCallback } from 'react';
import clipboard from 'copy-to-clipboard';

export interface UseClipboardOptions {
  format?: 'text/plain' | 'text/html';
  onCopy?: () => void;
}

export const useClipboard = ({ format = 'text/plain', onCopy }: UseClipboardOptions = {}): (text: string) => void => {
  return useCallback((text: string) => {
    clipboard(text, { format, onCopy, debug: true });
  }, [format, onCopy]);
};
