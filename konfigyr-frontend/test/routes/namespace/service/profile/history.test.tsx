import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, fireEvent, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | namespace | service | profile | history', () => {
  afterEach(() => cleanup());

  test('should render profile history page and navigate to next page', async () => {
    const { getByRole, getByText, router } = renderWithRouter('/namespace/konfigyr/services/konfigyr-api/profiles/development/history');

    await waitFor(() => {
      expect(getByText('Changeset draft')).toBeInTheDocument();
      expect(getByText('Test User <test.user@ebf.com>')).toBeInTheDocument();
    });

    expect(getByRole('navigation', { name: 'pagination' })).toBeInTheDocument();
    await userEvents.click(getByRole('button', { name: 'Go to next page' }));

    await waitFor(() => {
      expect(router.state.location.search.token).toStrictEqual('next-token');
    });
  });

  test('should render profile history page and select revision to view details', async () => {
    const { getByRole, getByText } = renderWithRouter('/namespace/konfigyr/services/konfigyr-api/profiles/development/history');

    await waitFor(() => {
      expect(getByText('Changeset draft')).toBeInTheDocument();
    });

    fireEvent.click(getByRole('button', { name: '9eadce4691d8fcd863aeeb07ef81d8146083d814' }));

    expect(getByRole('dialog', { name: 'Changeset draft' })).toBeInTheDocument();
  });

});
