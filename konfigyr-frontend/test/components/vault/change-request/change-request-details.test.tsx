import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { ChangeRequestDetails } from '@konfigyr/components/vault/change-request/change-request-details';
import { namespaces, services } from '@konfigyr/test/helpers/mocks';

describe('components | vault | change-request | <ChangeRequestDetails/>', () => {
  afterEach(() => cleanup());

  test('should render <ChangeRequestDetails/> component in loading state', () => {
    const { container } = renderWithQueryClient(
      <ChangeRequestDetails
        namespace={namespaces.konfigyr}
        service={services.konfigyrApi}
        number="1"
      />,
    );

    expect(container.querySelector('[data-slot="change-request-details-skeleton"]')).toBeInTheDocument();
  });

  test('should update change request name using the inline edit', async () => {
    const { getByRole } = renderWithQueryClient(
      <ChangeRequestDetails
        namespace={namespaces.konfigyr}
        service={services.konfigyrApi}
        number="1"
      />,
    );

    await waitFor(() => {
      expect(getByRole('button', { name: 'Update application name' })).toBeInTheDocument();
    });

    await userEvents.click(
      getByRole('button', { name: 'Update application name' }),
    );

    expect(getByRole('textbox', { name: 'Edit change request subject' })).toBeInTheDocument();

    await userEvents.type(
      getByRole('textbox', { name: 'Edit change request subject' }),
      ' - Updated',
    );

    await userEvents.keyboard('[Enter]');

    await waitFor(() => {
      expect(getByRole('button', { name: 'Update application name - Updated' })).toBeInTheDocument();
    });
  });

});
