// @vitest-environment node

import { describe, expect, test } from 'vitest';
import { Route } from '@konfigyr/routes/auth/code';
import { updateAuthorizationState } from '@konfigyr/test/helpers/session';
import { getRequestUrl } from '@tanstack/react-start/server';

const invokeRouter = (path: string = '') => {
  const url = new URL(`http://localhost${path}`);

  // @ts-expect-error: this is mocked by the @konfigyr/test/helpers/session helper
  getRequestUrl.mockReturnValue(url);

  // @ts-expect-error: have no better to invoke the loader function
  return Route.options.loader();
};

describe('routes | oauth | authorization-code', () => {
  test('throw an error when no authorization state is present', async () => {
    await expect(invokeRouter()).rejects.toThrow('Failed to exchange authorization code');
  });

  test('throw an error when authorization state does not match', async () => {
    await updateAuthorizationState({
      id: 'invalid-authorization-state-id',
      uri: 'http://localhost/',
      verifier: 'test-verifier',
    });

    await expect(invokeRouter('/auth/code?code=test-code&state=authorization-state-id'))
      .rejects.toThrow('Failed to exchange authorization code');
  });

  test('should consume authorization state and redirect to home page', async () => {
    await updateAuthorizationState({
      id: 'authorization-state-id',
      uri: 'http://localhost/',
      verifier: 'test-verifier',
    });

    await expect(invokeRouter('/auth/code?code=test-code&state=authorization-state-id'))
      .rejects.toThrow(Response);
  });
});
