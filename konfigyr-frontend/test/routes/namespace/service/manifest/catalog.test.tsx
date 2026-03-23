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
    const { getByRole, queryByText } = renderWithRouter('/namespace/konfigyr/services/konfigyr-api/manifest');

    await waitFor(async () => {
      await userEvent.type(
        getByRole('searchbox'),
        'spring.aop.auto',
      );
    });

    await waitFor(() => {
      expect(queryByText('spring.web.resources.chain.strategy.content.paths')).not.toBeInTheDocument();
    });
  });

  test('should search for properties and not find any match', async () => {
    const { getByRole, queryByText } = renderWithRouter('/namespace/konfigyr/services/konfigyr-api/manifest');

    await waitFor(async () => {
      await userEvent.type(
        getByRole('searchbox'),
        'missing property',
      );
    });

    await waitFor(() => {
      expect(queryByText('No matching properties found')).toBeInTheDocument();
    });
  });

  test('should render an empty service manifest properties page', async () => {
    const { getByText } = renderWithRouter('/namespace/konfigyr/services/konfigyr-id/manifest');

    await waitFor(() => {
      expect(getByText('No property metadata found')).toBeInTheDocument();
    });
  });

});
