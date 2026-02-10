import { afterAll, afterEach, beforeAll, describe, expect, test, vi } from 'vitest';
import { ServiceUpdateForm } from '@konfigyr/components/namespace/service/update-form';
import { Toaster } from '@konfigyr/components/ui/sonner';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { namespaces, services } from '@konfigyr/test/helpers/mocks';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';

import type { RenderResult } from '@testing-library/react';

describe('components | namespace | service | <ServiceUpdateForm/>', () => {
  let result: RenderResult;

  beforeAll(() => {
    result = renderWithQueryClient((
      <>
        <ServiceUpdateForm namespace={namespaces.konfigyr} service={services.konfigyrId} />
        <Toaster />
      </>
    ));
  });

  afterAll(() => cleanup());
  afterEach(() => vi.clearAllMocks());

  test('should render namespace service update form', () => {
    expect(result.getByRole('textbox', { name: 'Display name' })).toBeInTheDocument();
    expect(result.getByRole('textbox', { name: 'Description' })).toBeInTheDocument();

    expect(result.getByRole('button', { name: 'Update service' })).toBeInTheDocument();
  });

  test('should validate namespace service form', async () => {
    await userEvents.clear(
      result.getByRole('textbox', { name: 'Display name' }),
    );

    await userEvents.click(
      result.getByRole('button', { name: 'Update service' }),
    );

    expect(result.getByRole('textbox', { name: 'Display name' })).toBeInvalid();
    expect(result.getByRole('textbox', { name: 'Description' })).toBeValid();
    expect(result.getByRole('button')).toBeDisabled();
  });

  test('should show error notification when service can not be created', async () => {
    await userEvents.clear(
      result.getByRole('textbox', { name: 'Display name' }),
    );

    await userEvents.type(
      result.getByRole('textbox', { name: 'Display name' }),
      'invalid service name',
    );

    await userEvents.click(
      result.getByRole('button', { name: 'Update service' }),
    );

    await waitFor(() => {
      expect(result.getByText('Invalid service name: invalid service name.')).toBeInTheDocument();
    });
  });

  test('should update namespace service and show notification', async () => {
    await userEvents.clear(
      result.getByRole('textbox', { name: 'Display name' }),
    );

    await userEvents.type(
      result.getByRole('textbox', { name: 'Display name' }),
      'Service name',
    );

    await userEvents.click(
      result.getByRole('button', { name: 'Update service' }),
    );

    await waitFor(() => {
      expect(result.getByText('Your service was updated')).toBeInTheDocument();
    });
  });
});
