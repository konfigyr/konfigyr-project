import { afterAll, beforeAll, describe, expect, test } from 'vitest';
import { AccountNameForm } from '@konfigyr/components/account/name-form';
import { Toaster } from '@konfigyr/components/ui/sonner';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { accounts } from '@konfigyr/test/helpers/mocks';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';

import type { RenderResult } from '@testing-library/react';

describe('components | account | <AccountNameForm/>', () => {
  let result: RenderResult;

  beforeAll(() => {
    result = renderWithQueryClient((
      <>
        <AccountNameForm account={accounts.johnDoe} />
        <Toaster />
      </>
    ));
  });

  afterAll(() => cleanup());

  test('should render account name form', async () => {
    await waitFor(() => {
      expect(result.getByRole('textbox')).toBeInTheDocument();
      expect(result.getByRole('textbox')).toHaveAccessibleName('Display name');
      expect(result.getByRole('textbox')).toHaveAccessibleDescription(
        'This is how others will see you around the app. Keep it cool — or weird, we don’t judge.',
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

  test('should enter new account name', async () => {
    await userEvents.type(
      result.getByRole('textbox'),
      'Invalid display name',
    );

    expect(result.getByRole('textbox')).toBeValid();
    expect(result.getByRole('button')).not.toBeDisabled();
  });

  test('should submit form with invalid display name', async () => {
    await userEvents.click(
      result.getByRole('button'),
    );

    await waitFor(() => {
      expect(result.getByText('Bad request')).toBeInTheDocument();
    });

    expect(result.getByRole('textbox')).toBeValid();
    expect(result.getByRole('button')).not.toBeDisabled();
  });

  test('should submit form with valid display name', async () => {
    await userEvents.clear(
      result.getByRole('textbox'),
    );

    await userEvents.type(
      result.getByRole('textbox'),
      'John Doe',
    );

    await userEvents.click(
      result.getByRole('button'),
    );

    await waitFor(() => {
      expect(result.getByText('Your display name was updated')).toBeInTheDocument();
    });

    expect(result.getByRole('textbox')).toBeValid();
    expect(result.getByRole('button')).not.toBeDisabled();
  });
});
