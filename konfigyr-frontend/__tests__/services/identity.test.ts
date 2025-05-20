import { assert, beforeEach, describe, expect, test } from 'vitest';
import { NextRequest, NextResponse } from 'next/server';
import * as identity from 'konfigyr/services/identity';

describe('services | identity', () => {
  let request: NextRequest;
  let response : NextResponse;

  beforeEach(() => {
    request = new NextRequest('http:/localhost/test-uri');
    response = NextResponse.next();
  });

  test('should return an empty Identity value from request', async () => {
    const result = await identity.get(request);

    expect(result).toBeUndefined();
  });

  test('should check if Identity is present in the request', async () => {
    const result = await identity.has(request);

    expect(result).toStrictEqual(false);
  });

  test('should store and read Identity from the session store', async () => {
    const value = { email: 'paul.atreides@arakis.com' };
    await identity.set(response, value);

    expect(response.cookies.has('konfigyr.access')).toStrictEqual(true);

    const cookie = response.cookies.get('konfigyr.access');
    assert.isString(cookie?.value, 'Cookie value must be an encrypted string');
    assert.deepEqual(cookie?.name, 'konfigyr.access');
    assert.deepEqual(cookie?.httpOnly, true);
    assert.deepEqual(cookie?.secure, false);
    assert.deepEqual(cookie?.sameSite, 'lax');
    assert.deepEqual(cookie?.path, '/');

    // copy the cookies to request...
    response.cookies.getAll().forEach(cookie => {
      request.cookies.set(cookie.name, cookie.value);
    });

    expect(await identity.has(request)).toStrictEqual(true);
    expect(await identity.get(request)).toStrictEqual(value);
  });
});
