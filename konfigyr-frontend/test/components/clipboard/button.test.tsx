import { afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { ClipboardButton, ClipboardIconButton } from '@konfigyr/components/clipboard';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';

describe('components | clipboard | <ClipboardButton/>', () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  test('should render button with text to be copied', async () => {
    const { getByRole } = renderWithMessageProvider(<ClipboardButton text="text to be copied" />);

    expect(getByRole('button')).toBeDefined();
    expect(getByRole('button')).toHaveTextContent('Copy');

    await userEvents.click(getByRole('button'));

    await waitFor(() => {
      expect(getByRole('button')).toHaveTextContent('Copied');
    });

    expect(window.clipboardData.setData).toHaveBeenCalledExactlyOnceWith('text/plain', 'text to be copied');
  });

  test('should render button with text to be copied and present state in the tooltip', async () => {
    const { getByRole, queryByRole } = renderWithMessageProvider(<ClipboardIconButton text="text to be copied" />);

    expect(getByRole('button')).toBeDefined();
    expect(queryByRole('tooltip')).toBeNull();

    await userEvents.hover(getByRole('button'));

    expect(getByRole('tooltip')).toHaveTextContent('Copy');

    await userEvents.click(getByRole('button'));

    await waitFor(() => {
      expect(getByRole('tooltip')).toHaveTextContent('Copied');
    });

    expect(window.clipboardData.setData).toHaveBeenCalledExactlyOnceWith('text/plain', 'text to be copied');
  });

});
