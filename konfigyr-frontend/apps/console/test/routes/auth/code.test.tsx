// @vitest-environment node

import { describe, expect, test } from 'vitest';
import completeAuthorizationHandler from '@konfigyr/routes/auth/-handler';
import { updateAuthorizationState } from '@konfigyr/test/helpers/session';

const invokeRouter = async (path: string = '') => {
  const url = new URL(`http://localhost${path}`);
  return await completeAuthorizationHandler(url);
};

describe('routes | oauth | authorization-code', () => {
  test('create an error when no authorization state is present', async () => {
    const state = await invokeRouter();

    expect(state).toMatchObject({
      error: 'Authorization state is not present in the session',
      error_description: null,
      error_uri: null,
      retry_uri: null,
    });
  });

  test('create an error when authorization state does not match', async () => {
    await updateAuthorizationState({
      id: 'invalid-authorization-state-id',
      uri: 'http://localhost/',
      verifier: 'test-verifier',
    });

    const state = await invokeRouter(
      '/auth/code?code=test-code&state=authorization-state-id',
    );

    expect(state).toMatchObject({
      error: 'invalid response encountered',
      error_description: null,
      error_uri: null,
      retry_uri: 'http://localhost/',
    });
  });

  test('create an error when redirect URI contains OAuth error information', async () => {
    await updateAuthorizationState({
      id: 'error-state-id',
      uri: 'http://localhost/',
      verifier: 'test-verifier',
    });

    const state = await invokeRouter(
      '/auth/code?state=error-state-id&error=OAuth+error+message&error_description=OAuth+error+description&error_uri=error-uri',
    );

    expect(state).toMatchObject({
      error: 'OAuth error message',
      error_description: 'OAuth error description',
      error_uri: 'error-uri',
      retry_uri: 'http://localhost/',
    });
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
