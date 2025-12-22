// @vitest-environment node

import { describe, expect, test } from 'vitest';
import Authentication from '@konfigyr/lib/authentication';
import { deleteNamespaceHandler } from '@konfigyr/routes/_authenticated/namespace/$namespace/settings/-handler';
import { updateSessionAccessToken } from '@konfigyr/test/helpers/session';

describe('routes | namespace | settings | handler', () => {
  const data = { namespace: 'konfigyr' };

  test('should fail to delete namespace when authentication session is not present', async () => {
    const response = await deleteNamespaceHandler({ data });

    expect(response.status).toBe(401);

    await expect(response.json()).resolves.toMatchObject({
      status: 401,
      title: 'Session expired',
      detail: 'Please login again to delete your namespace.',
    });
  });

  test('should successfully delete namespace and redirect to provisioning page', async () => {
    await updateSessionAccessToken();

    const authentication = await Authentication.get();
    expect(authentication.authenticated).toBe(true);

    await expect(deleteNamespaceHandler({ data }))
      .rejects.toMatchObject({
        options: { to: '/namespace/provision', statusCode: 307 },
        status: 307,
      });
  });

  test('should successfully delete namespace and redirect to provided namespace dashboard', async () => {
    await updateSessionAccessToken();

    const authentication = await Authentication.get();
    expect(authentication.authenticated).toBe(true);

    await expect(deleteNamespaceHandler({ data: { ...data, redirect: 'john-doe' } }))
      .rejects.toMatchObject({
        options: {
          to: '/namespace/$namespace',
          params: { namespace: 'john-doe' },
          statusCode: 307,
        },
        status: 307,
      });
  });
});
