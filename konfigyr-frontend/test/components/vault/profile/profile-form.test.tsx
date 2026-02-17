import { afterAll, afterEach, beforeAll, describe, expect, test, vi } from 'vitest';
import { CreateProfileForm } from '@konfigyr/components/vault/profile/create-form';
import { Toaster } from '@konfigyr/components/ui/sonner';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { namespaces, services } from '@konfigyr/test/helpers/mocks';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';

import type { RenderResult } from '@testing-library/react';

describe('components | vault | profile | <CreateProfileForm/>', () => {
  const onCreate = vi.fn();
  let result: RenderResult;

  beforeAll(() => {
    result = renderWithQueryClient((
      <>
        <CreateProfileForm
          namespace={namespaces.konfigyr}
          service={services.konfigyrId}
          onCreate={onCreate}
        />
        <Toaster />
      </>
    ));
  });

  afterAll(() => cleanup());
  afterEach(() => vi.clearAllMocks());

  test('should render profile create form', () => {
    expect(result.getByRole('textbox', { name: 'Profile name' })).toBeInTheDocument();
    expect(result.getByRole('textbox', { name: 'Description' })).toBeInTheDocument();
    expect(result.getByRole('textbox', { name: 'Profile identifier' })).toBeInTheDocument();
    expect(result.getByRole('textbox', { name: 'Profile identifier' })).toHaveAccessibleDescription(
      'A unique identifier of the profile. Only lowercase letters, numbers, and hyphens are allowed.',
    );
    expect(result.getByRole('radiogroup')).toBeInTheDocument();
    expect(result.getByRole('radio', { name: 'Protected' })).toBeInTheDocument();
    expect(result.getByRole('radio', { name: 'Protected' })).toBeInTheDocument();
    expect(result.getByRole('radio', { name: 'Unprotected' })).toBeInTheDocument();
    expect(result.getByRole('spinbutton', { name: 'Display order' })).toBeInTheDocument();

    expect(result.getByRole('button', { name: 'Create profile' })).toBeInTheDocument();
  });

  test('should validate profile form', async () => {
    await userEvents.click(
      result.getByRole('button', { name: 'Create profile' }),
    );

    expect(result.getByRole('textbox', { name: 'Profile name' })).toBeInvalid();
    expect(result.getByRole('textbox', { name: 'Profile identifier' })).toBeInvalid();
    expect(result.getByRole('textbox', { name: 'Description' })).toBeValid();
    expect(result.getByRole('spinbutton', { name: 'Display order' })).toBeValid();
    expect(result.getByRole('button', { name: 'Create profile' })).toBeDisabled();
  });

  test('should show error notification when service can not be created', async () => {
    await userEvents.type(
      result.getByRole('textbox', { name: 'Profile identifier' }),
      'invalid-profile-identifier',
    );
    await userEvents.type(
      result.getByRole('textbox', { name: 'Profile name' }),
      'Profile name',
    );

    await userEvents.click(
      result.getByRole('button'),
    );

    await waitFor(() => {
      expect(result.getByText('Can not create profile')).toBeInTheDocument();
    });
  });

  test('should create profile and invoke the onCreate action', async () => {
    await userEvents.clear(
      result.getByRole('textbox', { name: 'Profile identifier' }),
    );

    await userEvents.type(
      result.getByRole('textbox', { name: 'Profile identifier' }),
      'profile-identifier',
    );

    await userEvents.click(
      result.getByRole('button'),
    );

    await waitFor(() => {
      expect(onCreate).toHaveBeenCalledOnce();
    });
  });
});
