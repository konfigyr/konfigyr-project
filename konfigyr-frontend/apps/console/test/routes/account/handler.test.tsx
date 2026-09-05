// @vitest-environment node

import { describe, expect, test } from 'vitest';
import Authentication from '@konfigyr/lib/authentication';
import { onDeleteAccount } from '@konfigyr/routes/_authenticated/account/-handler';
import { updateSessionAccessToken } from '@konfigyr/test/helpers/session';

describe('routes | account | handler', () => {
  test('should fail to delete account when authentication session is not present', async () => {
    const response = await onDeleteAccount();

    expect(response.status).toBe(401);
    expect(response.json()).resolves.toMatchObject({
      status: 401,
      title: 'Session expired',
      detail: 'Please login again to delete your account.',
    });
  });

  test('should successfully delete account', async () => {
    await updateSessionAccessToken();

    const authentication = await Authentication.get();
    expect(authentication.authenticated).toBe(true);

    const response = await onDeleteAccount();

    expect(response.status).toBe(204);
    expect(response.text()).resolves.toBe('');

    expect(authentication.authenticated).toBe(false);
  });
});
