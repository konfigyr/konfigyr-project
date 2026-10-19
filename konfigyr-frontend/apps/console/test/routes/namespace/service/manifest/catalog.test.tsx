import { afterEach, describe, expect, test } from 'vitest';
import { userEvent } from '@testing-library/user-event';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | namespace | service | manifest | properties', () => {
  afterEach(() => cleanup());

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
    const user = userEvent.setup();
    const { findByRole, getByText, queryByText, router } = renderWithRouter('/namespace/konfigyr/services/konfigyr-api/manifest');

    await waitFor(() => {
      expect(router.state.status).to.equal('idle');
    });

    // the service catalog is loaded by the component after the route is resolved
    const searchbox = await findByRole('searchbox');

    await user.type(searchbox, 'spring.aop.auto');

    await waitFor(() => {
      expect(getByText('spring.aop.auto')).toBeInTheDocument();
      expect(queryByText('spring.web.resources.chain.strategy.content.paths')).not.toBeInTheDocument();
    });

    await user.clear(searchbox);
    await user.type(searchbox, 'missing property');

    await waitFor(() => {
      expect(getByText('No matching properties found')).toBeInTheDocument();
    });
  });

  test('should render an empty service manifest properties page', async () => {
    const { getByText } = renderWithRouter('/namespace/konfigyr/services/konfigyr-id/manifest');

    await waitFor(() => {
      expect(getByText('No property metadata found')).toBeInTheDocument();
    });
  });

});
