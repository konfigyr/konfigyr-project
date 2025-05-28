import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import { NextIntlClientProvider } from 'next-intl';
import { HttpResponseError } from 'konfigyr/services/http';
import ErrorComponent from 'konfigyr/components/error';
import messages from '../../../messages/en.json';

describe('components/error', () => {
  afterEach(() => cleanup());

  test('should render generic error', () => {
    const error = new Error('Ooops, unexpected error occurred');

    const container = render(
      <NextIntlClientProvider locale="en" messages={messages}>
        <ErrorComponent error={error} />
      </NextIntlClientProvider>,
    );

    expect(screen.getByRole('alert')).toBeDefined();
    expect(screen.getByText('Unexpected server error occurred')).toBeDefined();
    expect(screen.getByText('We could not process your request due to a runtime error', { exact: false })).toBeDefined();

    expect(container.container.querySelector('.lucide-server-crash')).toBeInTheDocument();
  });

  test('should render HTTP response error', () => {
    const error = new HttpResponseError(
      new Response(null, { status: 404 }),
      { title: 'Not found', detail: 'Could not find resource' },
    );

    const container = render(
      <NextIntlClientProvider locale="en" messages={messages}>
        <ErrorComponent error={error} />
      </NextIntlClientProvider>,
    );

    expect(screen.getByRole('alert')).toBeDefined();
    expect(screen.getByText('Not found')).toBeDefined();
    expect(screen.getByText('Could not find resource', { exact: false })).toBeDefined();

    expect(container.container.querySelector('.lucide-search-x')).toBeInTheDocument();
  });
});
