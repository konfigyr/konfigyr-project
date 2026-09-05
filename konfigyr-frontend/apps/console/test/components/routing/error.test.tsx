import { HTTPError } from 'ky';
import { afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup } from '@testing-library/react';
import { ErrorBoundary } from '@konfigyr/components/routing/error';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';

import type { NormalizedOptions } from 'ky';

describe('components | routing | <ErrorBoundary />', () => {
  const reset = vi.fn();

  afterEach(() => cleanup());

  test('render the default error boundary component', () => {
    const error = new Error('Test error');

    const { getByRole, getByText } = renderComponentWithRouter(<ErrorBoundary error={error} reset={reset} />);

    expect(getByText(error.message)).toBeInTheDocument();
    expect(getByText('Unexpected server error occurred')).toBeInTheDocument();
    expect(getByRole('link', { name: 'Contact our support team' })).toBeInTheDocument();
    expect(getByRole('link', { name: 'Home' })).toBeInTheDocument();
  });

  test('render the HTTP error boundary component', () => {
    const error = new HTTPError(
      vi.fn() as unknown as Response,
      vi.fn() as unknown as Request,
      vi.fn() as unknown as NormalizedOptions,
    );

    error.problem = { title: 'Test error', detail: 'Test detail' };

    const { getByText } = renderComponentWithRouter(<ErrorBoundary error={error} reset={reset} />);

    expect(getByText(error.message)).toBeInTheDocument();
    expect(getByText('Test error')).toBeInTheDocument();
    expect(getByText('Test detail')).toBeInTheDocument();
  });
});
