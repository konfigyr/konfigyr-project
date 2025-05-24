import { describe, expect, test } from 'vitest';
import { render, screen } from '@testing-library/react';
import { NextIntlClientProvider } from 'next-intl';
import ErrorPage from 'konfigyr/app/error';
import messages from '../../messages/en.json';

describe('app/error', () => {
  test('should render error page', () => {
    const error = new Error('Oops, an error occurred');
    render(
      <NextIntlClientProvider locale="en" messages={messages}>
        <ErrorPage error={error} />
      </NextIntlClientProvider>,
    );

    expect(screen.getByRole('alert')).toBeDefined();
  });
});
