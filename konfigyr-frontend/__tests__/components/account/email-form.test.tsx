import { afterEach, describe, expect, Mock, test, vi } from 'vitest';
import userEvent from '@testing-library/user-event';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { NextIntlClientProvider } from 'next-intl';
import { AccountContextProvider, AccountEmailForm } from 'konfigyr/components/account';
import messages from '../../../messages/en.json';

const account = { id: 'test-account', email: 'john.doe@konfigyr.com', fullName: 'John Doe' };

function TestForm() {
  return (
    <NextIntlClientProvider locale="en" messages={messages}>
      <AccountContextProvider account={account}>
        <AccountEmailForm />
      </AccountContextProvider>
    </NextIntlClientProvider>
  );
}

describe('components/account/email-form', () => {
  afterEach(() => {
    vi.resetAllMocks();
    cleanup();
  });

  test('should render account email form', async () => {
    render(<TestForm />);

    expect(screen.getByRole('form')).toBeDefined();
    expect(screen.getByRole('textbox')).toBeDefined();
    expect(screen.getByRole('textbox')).toHaveValue('john.doe@konfigyr.com');
    expect(screen.getByRole('textbox')).toHaveAccessibleName('Email address');
    expect(screen.getByRole('textbox')).toHaveAccessibleDescription(
      'We will use this to send you important stuff — like password resets, not cat memes. Promise.',
    );
    expect(screen.getByText('Please use 48 characters at maximum.')).toBeDefined();
    expect(screen.getByRole('button')).toBeDefined();
    expect(screen.getByRole('button')).toHaveAttribute('type', 'submit');
  });

  test('should update account email address', async () => {
    global.fetch = vi.fn((_: string, request: Request) => {
      let body;

      if (request.method === 'POST') {
        body = { token: 'verification-token' };
      } else if (request.method === 'PUT') {
        body = { ...account, email: 'updated@konfigyr.com' };
      } else {
        expect.fail('Expected request method of either POST or PUT');
      }

      return Promise.resolve({
        status: 200,
        json: () => Promise.resolve(body),
      });
    }) as Mock;

    render(<TestForm />);

    await userEvent.clear(screen.getByRole('textbox'));
    await userEvent.type(screen.getByRole('textbox'), 'j.doe@konfigyr.com');
    await fireEvent.submit(screen.getByRole('button'));

    const code = await screen.findByRole('textbox', { name: 'Enter confirmation code' });

    expect(code).toBeInTheDocument();
    expect(code).toHaveAccessibleDescription(
      'We have sent you a confirmation code to your new email address, ' +
      'please paste it here to confirm your email address change.',
    );

    expect(screen.getByRole('textbox', { name: 'Email address' })).toHaveValue('j.doe@konfigyr.com');
    expect(screen.getByRole('textbox', { name: 'Email address' })).toBeDisabled();

    await userEvent.type(code, '123456');
    await fireEvent.submit(screen.getByRole('button'));

    expect(await screen.findByRole('textbox', { name: 'Enter confirmation code' })).not.toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Email address' })).toHaveValue('updated@konfigyr.com');
    expect(screen.getByRole('textbox', { name: 'Email address' })).not.toBeDisabled();
  });

});
