import { describe, expect, test, vi } from 'vitest';
import { ErrorBoundary } from '@konfigyr/components/routing/error';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';

describe('components | routing | <ErrorBoundary />', () => {
  test('render the default error boundary component', () => {
    const error = new Error('Test error');
    const reset = vi.fn();

    const { getByRole, getByText } = renderComponentWithRouter(<ErrorBoundary error={error} reset={reset} />);

    expect(getByText(error.message)).toBeInTheDocument();
    expect(getByRole('button', { name: 'Try Again' })).toBeInTheDocument();
    expect(getByRole('link', { name: 'Home' })).toBeInTheDocument();
  });
});
