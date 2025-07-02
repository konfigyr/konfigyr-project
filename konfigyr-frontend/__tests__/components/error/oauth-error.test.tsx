import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import { NextIntlClientProvider } from 'next-intl';
import { ErrorCodes } from 'konfigyr/services/openid';
import OAuthErrorCard from 'konfigyr/components/error/oauth-error';
import messages from '../../../messages/en.json';

describe('app/auth/error', () => {

  afterEach(() => cleanup());

  test('should render error card using default error details', () => {
    const container = render(
      <NextIntlClientProvider locale="en" messages={messages}>
        <OAuthErrorCard />
      </NextIntlClientProvider>,
    );

    expect(screen.getByText('Authorization error')).toBeDefined();

    expect(screen.getByText(
      'Unexpected server error occurred while logging you in. ' +
      'Please try again and if the problem persists, please get in touch with our support team.',
    )).toBeDefined();

    const links = container.getAllByRole('link');
    expect(links).toHaveLength(2);

    const login = links[0];
    expect(login).toHaveAttribute('href', '/auth/authorize');
    expect(login).toHaveTextContent('Login');

    const support = links[1];
    expect(support).toHaveAttribute('href', 'mailto:support@konfigyr.com');
    expect(support).toHaveTextContent('Contact our support team');
  });

  test('should render error card using OAuth error details', () => {
    const container = render(
      <NextIntlClientProvider locale="en" messages={messages}>
        <OAuthErrorCard
          code={ErrorCodes.RESPONSE_BODY_ERROR}
          error="oauth-error-code"
          error_description="Some error description"
          error_uri="https://oauth.com/error-link"
        />
      </NextIntlClientProvider>,
    );

    expect(screen.getByText('Authorization error')).toBeDefined();

    expect(screen.getByText(
      'Authorization server returned an error. Please checkout the details below.',
    )).toBeDefined();

    const paragraphs = container.getAllByRole('paragraph');
    expect(paragraphs).toHaveLength(2);

    expect(paragraphs[0]).toHaveTextContent('Error code: oauth-error-code');
    expect(paragraphs[1]).toHaveTextContent('Error description: Some error description');

    const links = container.getAllByRole('link');
    expect(links).toHaveLength(3);

    const uri = links[0];
    expect(uri).toHaveAttribute('href', 'https://oauth.com/error-link');
    expect(uri).toHaveTextContent('Find out more here');

    const login = links[1];
    expect(login).toHaveAttribute('href', '/auth/authorize');
    expect(login).toHaveTextContent('Login');

    const support = links[2];
    expect(support).toHaveAttribute('href', 'mailto:support@konfigyr.com');
    expect(support).toHaveTextContent('Contact our support team');
  });
});
