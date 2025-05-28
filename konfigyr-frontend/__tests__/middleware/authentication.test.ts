import {  describe, expect, test, vi } from 'vitest';
import { NextRequest } from 'next/server';
import isAuthenticated from 'konfigyr/middleware/authentication';
import * as session from 'konfigyr/services/session';

const service = vi.hoisted(() => {
  return { isAuthenticated: vi.fn() };
});

vi.mock('konfigyr/services/authentication', () => ({
  isAuthenticated: service.isAuthenticated,
}));

const check = (path: string) => isAuthenticated(
  new NextRequest(new URL(path, 'http://localhost')),
);

describe('middleware | csp', () => {
  test('should ignore whitelisted URLs', async () => {
    const response = await check('/');

    expect(response).toBeNull();
  });

  test('should ignore when already authenticated', async () => {
    service.isAuthenticated.mockResolvedValue(true);

    const response = await check('/namespace/konfigyr');

    expect(response).toBeNull();
  });

  test('should throw error when authentication session check fails', async () => {
    service.isAuthenticated.mockRejectedValue('Failure');

    expect(check('/namespace/konfigyr')).rejects.toBe('Failure');
  });

  test('should redirect to authorization when not authenticated', async () => {
    service.isAuthenticated.mockResolvedValue(false);

    const response = await check('/namespace/konfigyr');

    expect(response).not.toBeNull();
    expect(response?.status).toStrictEqual(307);
    expect(response?.headers.get('location')).toStrictEqual('http://localhost/auth/authorize');

    const url = await session.get(response!.cookies, 'attempted-request');

    expect(url).toStrictEqual('http://localhost/namespace/konfigyr');
  });
});
