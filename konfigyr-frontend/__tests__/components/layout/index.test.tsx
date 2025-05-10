import { describe, expect, test } from 'vitest';
import { NextIntlClientProvider } from 'next-intl';
import { render, screen } from '@testing-library/react';
import Layout from 'konfigyr/components/layout';
import messages from '../../../messages/en.json';

describe('components/layout', () => {
  const container = render(
    <NextIntlClientProvider locale="en" messages={messages}>
      <Layout>
        <h1>Title</h1>
      </Layout>
    </NextIntlClientProvider>,
  );

  test('should render service label', () => {
    const links = container.getAllByRole('link');
    expect(links).toHaveLength(2);

    const label = links[0];
    expect(label).toHaveAttribute('href', '/');
    expect(label).toHaveTextContent('konfigyr.vault');
  });

  test('should render login link', () => {
    const links = container.getAllByRole('link');
    expect(links).toHaveLength(2);

    const login = links[1];
    expect(login).toHaveAttribute('href', '/auth/authorize');
    expect(login).toHaveTextContent('Login');
  });

  test('should render layout with child element', () => {
    expect(screen.getByRole('heading', { level: 1, name: 'Title' })).toBeDefined();
  });
});
