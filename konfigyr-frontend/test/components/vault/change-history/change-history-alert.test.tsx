import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import { ChangeHistoryAlert } from '@konfigyr/components/vault/change-history/change-history-alert';
import { namespaces, profiles, services } from '@konfigyr/test/helpers/mocks';

describe('components | vault | change-history | <ChangeHistoryAlert/>', () => {
  afterEach(() => cleanup());

  test('should render <ChangeHistoryAlert/> component', async () => {
    const { getByRole, getByText } = renderComponentWithRouter(
      <ChangeHistoryAlert
        namespace={namespaces.konfigyr}
        service={services.konfigyrApi}
        profile={profiles.development}
      />,
    );

    await waitFor(() => {
      expect(getByRole('log', { name: 'Changeset draft' })).toBeInTheDocument();
    });

    expect(getByText('Test User <test.user@ebf.com>')).toBeInTheDocument();
    expect(getByText('Changeset draft')).toBeInTheDocument();
    expect(getByText('5 days ago')).toBeInTheDocument();

    expect(getByRole('link', { name: 'View history' })).toBeInTheDocument();
    expect(getByRole('link', { name: 'View history' })).toHaveAttribute(
      'href', `/namespace/${namespaces.konfigyr.slug}/services/${services.konfigyrApi.slug}/profiles/${profiles.development.slug}/history`,
    );
  });
});
