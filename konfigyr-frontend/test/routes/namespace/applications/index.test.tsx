import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';
import userEvents from '@testing-library/user-event/dist/cjs/index.js';


describe('routes | namespace | applications', () => {
  afterEach(() => cleanup());

  test('should render namespace application page without applications', async () => {
    const { getByText } = renderWithRouter('/namespace/john-doe/applications');

    await waitFor(() => {
      expect(getByText('Your namespace has no applications yet.')).toBeInTheDocument();
    });
  });

  test('should render applications list', async () => {
    const { getByText } = renderWithRouter('/namespace/konfigyr/applications');

    await waitFor(() => {
      expect(getByText('konfigyr test'), 'render name of the application').toBeInTheDocument();
      expect(getByText('This application has no expiration date'), 'render expiration date of the application').toBeInTheDocument();
      expect(getByText('Delete application')).toBeInTheDocument();
    });

  });

  test.skip('should delete namespace application', async () => {
    const { getByText, getByRole } = renderWithRouter('/namespace/konfigyr/applications');

    await waitFor(() => {
      expect(getByText('konfigyr test')).toBeInTheDocument();
      expect(getByText('Delete application')).toBeInTheDocument();
    });

    await userEvents.click(
      getByRole('button', {
        name: 'Delete application',
      }),
    );

    await waitFor(() => {
      expect(getByText('Delete "konfigyr test" application'), 'render title of the confirmation window').toBeInTheDocument();
      expect(getByText('Are you sure you want to delete "konfigyr test" application? This action cannot be undone.'), 'render body of the confirmation window').toBeInTheDocument();
    });

    await userEvents.click(
      getByRole('button', {
        name: 'Yes',
      }),
    );
  });
});
