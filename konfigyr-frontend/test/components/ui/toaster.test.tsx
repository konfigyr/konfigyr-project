import { afterEach, describe, expect, test } from 'vitest';
import { act, cleanup, render, waitFor } from '@testing-library/react';
import { Toaster, toast } from '@konfigyr/components/ui/toast';

describe('components | UI | <Toaster/>', () => {
  afterEach(() => cleanup());

  test('should render Toast notification container', async () => {
    const { getByLabelText, getByText } = render(<Toaster data-testid="container"/>);

    expect(getByLabelText('Notifications')).toBeInTheDocument();

    await act(() => toast.add({ title: 'Test toast' }));

    await waitFor(() => {
      expect(getByText('Test toast')).toBeInTheDocument();
    });
  });
});
