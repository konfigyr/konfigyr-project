import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithRouter } from '@konfigyr/test/helpers/router';

describe('routes | namespace | applications', () => {
  afterEach(() => cleanup());

  test('should render namespace application page without applications', async () => {
    const { getByRole, getByText } = renderWithRouter('/namespace/john-doe/applications');

    await waitFor(() => {
      expect(getByRole('link', { name: 'Create application' })).toBeInTheDocument();
      expect(getByText('Your namespace has no applications yet.')).toBeInTheDocument();
    });
  });

  test('should render applications list', async () => {
    const { getByRole, getByText } = renderWithRouter('/namespace/konfigyr/applications');

    await waitFor(() => {
      expect(getByRole('link', { name: 'Create application' })).toBeInTheDocument();
      expect(getByText('konfigyr test')).toBeInTheDocument();
      expect(getByText('This application has no expiration date')).toBeInTheDocument();
    });
  });

});
