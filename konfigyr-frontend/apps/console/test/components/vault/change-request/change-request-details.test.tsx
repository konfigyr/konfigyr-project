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

  test('should render loaded change request with actions and property changes', async () => {
    const { getByRole, getByText } = renderWithQueryClient(
      <ChangeRequestDetails
        namespace={namespaces.konfigyr}
        service={services.konfigyrApi}
        number="1"
      />,
    );

    await waitFor(() => {
      expect(getByText('Update application name')).toBeInTheDocument();
    });

    expect(getByRole('button', { name: 'Merge changes' })).toBeInTheDocument();
    expect(getByRole('button', { name: 'Submit review' })).toBeInTheDocument();
    expect(getByRole('button', { name: 'Discard change request' })).toBeInTheDocument();

    await waitFor(() => {
      expect(getByText('logging.file.max-size')).toBeInTheDocument();
      expect(getByText('logging.level.com.konfigyr.test')).toBeInTheDocument();
      expect(getByText('logging.file.max-history')).toBeInTheDocument();
    });
  });

  test('should render an error state when the change request is not found', async () => {
    const { getByText } = renderWithQueryClient(
      <ChangeRequestDetails
        namespace={namespaces.konfigyr}
        service={services.konfigyrApi}
        number="2"
      />,
    );

    await waitFor(() => {
      expect(getByText('Change request not found')).toBeInTheDocument();
    });
  });

  test('should update change request name using the inline edit', async () => {
    const user = userEvents.setup();
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

    await user.click(
      getByRole('button', { name: 'Update application name' }),
    );

    expect(getByRole('textbox', { name: 'Edit change request subject' })).toBeInTheDocument();

    await user.type(
      getByRole('textbox', { name: 'Edit change request subject' }),
      ' - Updated',
    );

    await user.keyboard('[Enter]');

    await waitFor(() => {
      expect(getByRole('button', { name: 'Update application name - Updated' })).toBeInTheDocument();
    });
  });

  test('should update change request description', async () => {
    const user = userEvents.setup();
    const { getByRole, getByText } = renderWithQueryClient(
      <ChangeRequestDetails
        namespace={namespaces.konfigyr}
        service={services.konfigyrApi}
        number="1"
      />,
    );

    await waitFor(() => {
      expect(getByRole('button', { name: 'Edit' })).toBeInTheDocument();
    });

    expect(getByText('Align the application name with the new naming convention')).toBeInTheDocument();

    await user.click(
      getByRole('button', { name: 'Edit' }),
    );

    expect(getByRole('textbox', { name: 'Update change request description' })).toBeInTheDocument();

    await user.type(
      getByRole('textbox', { name: 'Update change request description' }),
      ' - Updated description',
    );

    await user.click(
      getByRole('button', { name: 'Update description' }),
    );

    await waitFor(() => {
      expect(getByText('Align the application name with the new naming convention - Updated description'))
        .toBeInTheDocument();
    });
  });

});
