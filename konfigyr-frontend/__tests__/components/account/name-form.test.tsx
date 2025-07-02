import { afterEach, describe, expect, Mock, test, vi } from 'vitest';
import userEvent from '@testing-library/user-event';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { NextIntlClientProvider } from 'next-intl';
import { AccountContextProvider, AccountNameForm } from 'konfigyr/components/account';
import messages from '../../../messages/en.json';

const account = { id: 'test-account', email: 'john.doe@konfigyr.com', fullName: 'John Doe' };

function TestForm() {
  return (
    <NextIntlClientProvider locale="en" messages={messages}>
      <AccountContextProvider account={account}>
        <AccountNameForm />
      </AccountContextProvider>
    </NextIntlClientProvider>
  );
}

describe('components/account/name-form', () => {
  afterEach(() => {
    vi.resetAllMocks();
    cleanup();
  });

  test('should render account name form', async () => {
    render(<TestForm />);

    expect(screen.getByRole('form')).toBeDefined();
    expect(screen.getByRole('textbox')).toBeDefined();
    expect(screen.getByRole('textbox')).toHaveValue('John Doe');
    expect(screen.getByRole('textbox')).toHaveAccessibleName('Display name');
    expect(screen.getByRole('textbox')).toHaveAccessibleDescription(
      'This is how others will see you around the app. Keep it cool — or weird, we don’t judge.',
    );
    expect(screen.getByText('Please use 32 characters at maximum.')).toBeDefined();
    expect(screen.getByRole('button')).toBeDefined();
    expect(screen.getByRole('button')).toHaveAttribute('type', 'submit');
  });

  test('should update account display name', async () => {
    global.fetch = vi.fn((_: string, request: Request) => {
      const { name } = JSON.parse(String(request.body));

      return Promise.resolve({
        status: 200,
        json: () => Promise.resolve({ ...account, fullName: name }),
      });
    }) as Mock;

    render(<TestForm />);

    await userEvent.clear(screen.getByRole('textbox'));
    await userEvent.type(screen.getByRole('textbox'), 'Changed name');
    await fireEvent.submit(screen.getByRole('button'));

    expect(screen.getByRole('textbox')).toHaveValue('Changed name');
  });

});
