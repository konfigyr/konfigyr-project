import { afterAll, beforeAll, describe, expect, test } from 'vitest';
import { AccountEmailForm } from '@konfigyr/components/account/email-form';
import { Toaster } from '@konfigyr/components/ui/sonner';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { accounts } from '@konfigyr/test/helpers/mocks';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';

import type { RenderResult } from '@testing-library/react';

describe('components | account | <AccountEmailForm/>', () => {
  let result: RenderResult;

  beforeAll(() => {
    result = renderWithQueryClient((
      <>
        <AccountEmailForm account={accounts.johnDoe} />
        <Toaster />
      </>
    ));
  });

  afterAll(() => cleanup());

  test('should render account email form', async () => {
    await waitFor(() => {
      expect(result.getByRole('textbox')).toBeInTheDocument();
      expect(result.getByRole('textbox')).toHaveAccessibleName('Email address');
      expect(result.getByRole('textbox')).toHaveAccessibleDescription(
        'We will use this to send you important stuff — like password resets, not cat memes. Promise.',
      );
    });

    await waitFor(() => {
      expect(result.getByRole('button', { name: 'Update' })).toBeInTheDocument();
    });
  });

  test('should fail to submit invalid form', async () => {
    await userEvents.clear(
      result.getByRole('textbox'),
    );

    await userEvents.click(
      result.getByRole('button'),
    );

    expect(result.getByRole('textbox')).toBeInvalid();
    expect(result.getByRole('button')).toBeDisabled();
  });

  test('should enter new account email', async () => {
    await userEvents.type(
      result.getByRole('textbox'),
      'unavilable-email@konfigyr.com',
    );

    expect(result.getByRole('textbox')).toBeValid();
    expect(result.getByRole('button')).not.toBeDisabled();
  });

  test('should submit form with invalid email address', async () => {
    await userEvents.click(
      result.getByRole('button'),
    );

    await waitFor(() => {
      expect(result.getByText('Bad request')).toBeInTheDocument();
    });

    expect(result.getByRole('textbox')).toBeValid();
    expect(result.getByRole('button')).not.toBeDisabled();
  });

  test('should submit form with valid email address', async () => {
    await userEvents.clear(
      result.getByRole('textbox'),
    );

    await userEvents.type(
      result.getByRole('textbox'),
      'valid-address@konfigyr.com',
    );

    await userEvents.click(
      result.getByRole('button'),
    );

    await waitFor(() => {
      expect(result.getByRole('textbox', { name: 'Enter confirmation code' })).toBeInTheDocument();
      expect(result.getByRole('textbox', { name: 'Enter confirmation code' })).toHaveAccessibleDescription(
        'We have sent you a confirmation code to your new email address, please paste it here to confirm your email address change.',
      );
    });

    expect(result.getByRole('button')).not.toBeDisabled();
  });

  test('should submit form with invalid verification code', async () => {
    await userEvents.type(
      result.getByRole('textbox', { name: 'Enter confirmation code' }),
      'invalid-code',
    );

    await userEvents.click(
      result.getByRole('button'),
    );

    await waitFor(() => {
      expect(result.getByText('Invalid verification code.')).toBeInTheDocument();
    });
  });

  test('should submit form with valid verification code', async () => {
    await userEvents.clear(
      result.getByRole('textbox', { name: 'Enter confirmation code' }),
    );

    await userEvents.type(
      result.getByRole('textbox', { name: 'Enter confirmation code' }),
      'valid-code',
    );

    await userEvents.click(
      result.getByRole('button'),
    );

    await waitFor(() => {
      expect(result.getByText('Your email address was updated')).toBeInTheDocument();
    });
  });
});
