import { afterEach, describe, expect, test, vi } from 'vitest';
import { userEvent } from '@testing-library/user-event';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | namespace | service | manifest | properties', () => {
  afterEach(() => {
    cleanup();
    vi.useRealTimers();
  });

  test('should render service manifest properties page', async () => {
    const { getByText } = renderWithRouter('/namespace/konfigyr/services/konfigyr-api/manifest');

    await waitFor(() => {
      expect(getByText('spring.aop.auto')).toBeInTheDocument();
      expect(getByText('Add @EnableAspectJAutoProxy.')).toBeInTheDocument();

      expect(getByText('spring.web.resources.chain.strategy.content.paths')).toBeInTheDocument();
      expect(getByText('List of patterns to apply to the content Version Strategy.')).toBeInTheDocument();
    });
  });

  test('should search for properties', async () => {
    const { getByRole, queryByText, router } = renderWithRouter('/namespace/konfigyr/services/konfigyr-api/manifest');

    await waitFor(() => {
      expect(router.state.status).to.equal('idle');
    });

    vi.useFakeTimers();

    const user = userEvent.setup({
      delay: null,
      advanceTimers: vi.advanceTimersByTime,
    });

    user.type(
      getByRole('searchbox'),
      'spring.aop.auto',
    );

    vi.advanceTimersByTime(500);

    expect(queryByText('spring.web.resources.chain.strategy.content.paths')).not.toBeInTheDocument();

    user.type(
      getByRole('searchbox'),
      'missing property',
    );

    vi.advanceTimersByTime(500);

    expect(queryByText('No matching properties found')).toBeInTheDocument();
  });

  test('should render an empty service manifest properties page', async () => {
    const { getByText } = renderWithRouter('/namespace/konfigyr/services/konfigyr-id/manifest');

    await waitFor(() => {
      expect(getByText('No property metadata found')).toBeInTheDocument();
    });
  });

});
