import { toast } from 'sonner';
import { afterEach, describe, expect, test } from 'vitest';
import { act, cleanup, render, waitFor } from '@testing-library/react';
import { Toaster } from '@konfigyr/components/ui/sonner';

describe('components | UI | <Toaster/>', () => {
  afterEach(() => cleanup());

  test('should render Toast notification container', async () => {
    const { getByLabelText, getByText } = render(<Toaster data-testid="container"/>);

    expect(getByLabelText('Notifications alt+T')).toBeInTheDocument();

    act(() => toast('Test toast'));

    await waitFor(() => {
      expect(getByText('Test toast')).toBeInTheDocument();
    });
  });
});
