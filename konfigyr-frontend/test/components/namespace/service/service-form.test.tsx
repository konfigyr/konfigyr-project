import { afterAll, afterEach, beforeAll, describe, expect, test, vi } from 'vitest';
import { CreateServiceForm } from '@konfigyr/components/namespace/service/service-form';
import { Toaster } from '@konfigyr/components/ui/sonner';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { namespaces } from '@konfigyr/test/helpers/mocks';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';

import type { RenderResult } from '@testing-library/react';

describe('components | namespace | service | <CreateServiceForm/>', () => {
  const onCreate = vi.fn();
  let result: RenderResult;

  beforeAll(() => {
    result = renderWithQueryClient((
      <>
        <CreateServiceForm namespace={namespaces.konfigyr} onCreate={onCreate}/>
        <Toaster />
      </>
    ));
  });

  afterAll(() => cleanup());
  afterEach(() => vi.clearAllMocks());

  test('should render namespace service create form', () => {
    expect(result.getByRole('textbox', { name: 'Identifier' })).toBeInTheDocument();
    expect(result.getByRole('textbox', { name: 'Identifier' })).toHaveAccessibleDescription(
      'This is your service’s unique identifier within this namespace. It must be identical to your application\'s configured name to ensure properties are loaded correctly.',
    );

    expect(result.getByRole('textbox', { name: 'Display name' })).toBeInTheDocument();
    expect(result.getByRole('textbox', { name: 'Description' })).toBeInTheDocument();

    expect(result.getByRole('button', { name: 'Create service' })).toBeInTheDocument();
  });

  test('should validate namespace service form', async () => {
    await userEvents.click(
      result.getByRole('button', { name: 'Create service' }),
    );

    expect(result.getByRole('textbox', { name: 'Identifier' })).toBeInvalid();
    expect(result.getByRole('textbox', { name: 'Display name' })).toBeInvalid();
    expect(result.getByRole('textbox', { name: 'Description' })).toBeValid();
    expect(result.getByRole('button')).toBeDisabled();
  });

  test('should show error notification when service can not be created', async () => {
    await userEvents.type(
      result.getByRole('textbox', { name: 'Identifier' }),
      'invalid-service-identifier',
    );

    await userEvents.type(
      result.getByRole('textbox', { name: 'Display name' }),
      'Service name',
    );

    await userEvents.click(
      result.getByRole('button'),
    );

    await waitFor(() => {
      expect(result.getByText('Can not create service')).toBeInTheDocument();
    });
  });

  test('should create namespace service and invoke the onCreate action', async () => {
    await userEvents.clear(
      result.getByRole('textbox', { name: 'Identifier' }),
    );

    await userEvents.type(
      result.getByRole('textbox', { name: 'Identifier' }),
      'service-identifier',
    );

    await userEvents.click(
      result.getByRole('button'),
    );

    await waitFor(() => {
      expect(onCreate).toHaveBeenCalledOnce();
    });
  });
});
