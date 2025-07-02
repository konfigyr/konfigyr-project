import type { Account } from 'konfigyr/services/authentication';

import { afterEach, describe, expect, test } from 'vitest';
import { NextIntlClientProvider } from 'next-intl';
import { cleanup, fireEvent, render } from '@testing-library/react';
import Layout from 'konfigyr/components/layout';
import { AccountContextProvider } from 'konfigyr/components/account';
import messages from '../../../messages/en.json';

function TestLayout({ account }: { account?: Account }) {
  return (
    <NextIntlClientProvider locale="en" messages={messages}>
      <AccountContextProvider account={account}>
        <Layout>
          <h1>Title</h1>
        </Layout>
      </AccountContextProvider>
    </NextIntlClientProvider>
  );
}

describe('components/layout', () => {
  afterEach(() => cleanup());

  test('should render service label and child elements', () => {
    const container = render(<TestLayout />);
    const links = container.getAllByRole('link');
    expect(links).toHaveLength(2);

    const label = links[0];
    expect(label).toHaveAttribute('href', '/');
    expect(label).toHaveTextContent('konfigyr.vault');

    expect(container.getByRole('heading', { level: 1, name: 'Title' })).toBeDefined();
  });

  test('should render login link', () => {
    const container = render(<TestLayout />);
    const links = container.getAllByRole('link');
    expect(links).toHaveLength(2);

    const login = links[1];
    expect(login).toHaveAttribute('href', '/auth/authorize');
    expect(login).toHaveTextContent('Login');
  });

  test('should render account dropdown', async () => {
    const account = { id: 'test', email: 'john.doe@konfigyr.com', fullName: 'John Doe' };
    const container = render(<TestLayout account={account} />);
    const links = container.getAllByRole('link');
    expect(links).toHaveLength(1);

    const button = container.getByRole('button');
    expect(button).toBeDefined();
    expect(button).toHaveTextContent(account.fullName);
    expect(button).toHaveAttribute('data-state', 'closed');

    expect(container.queryByText(account.email)).not.toBeInTheDocument();
    expect(container.queryByText('Account settings')).not.toBeInTheDocument();
    expect(container.queryByText('Logout')).not.toBeInTheDocument();

    fireEvent.keyDown(button, { key: ' ' });

    expect(container.getByText(account.email)).toBeInTheDocument();
    expect(container.getByText('Account settings')).toBeInTheDocument();
    expect(container.getByText('Logout')).toBeInTheDocument();
  });

});
