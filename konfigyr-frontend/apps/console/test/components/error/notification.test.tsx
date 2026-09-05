import { HTTPError } from 'ky';
import { afterEach, describe, expect, test, vi } from 'vitest';
import { act, cleanup, renderHook } from '@testing-library/react';
import { useErrorNotification } from '@konfigyr/components/error/notification';
import { GeneralErrorDetail, GeneralErrorTitle } from '@konfigyr/components/messages';

import type { NormalizedOptions } from 'ky';

const toast = vi.hoisted(() => ({
  add: vi.fn(),
}));

vi.mock('@konfigyr/components/ui/toast', () => ({ toast }));

describe('components | error | notification', () => {
  afterEach(() => {
    vi.resetAllMocks();
    cleanup();
  });

  test('should show notification for a runtime error', () => {
    const error = new Error('Test error');
    const { result } = renderHook(() => useErrorNotification());

    expect(result.current).toBeInstanceOf(Function);

    act(() => result.current(error));

    expect(toast.add).toHaveBeenCalledExactlyOnceWith({
      type: 'error', title: <GeneralErrorTitle />, description: <GeneralErrorDetail />,
    });
  });

  test('should show notification for an HTTP error', () => {
    const error = new HTTPError(
      vi.fn() as unknown as Response,
      vi.fn() as unknown as Request,
      vi.fn() as unknown as NormalizedOptions,
    );

    error.problem = { title: 'Test error', detail: 'Test detail' };

    const { result } = renderHook(() => useErrorNotification({
      timeout: 1000,
    }));

    expect(result.current).toBeInstanceOf(Function);

    act(() => result.current(error));

    expect(toast.add).toHaveBeenCalledExactlyOnceWith({
      timeout: 1000, type: 'error', title: error.problem.title, description: error.problem.detail,
    });
  });
});
