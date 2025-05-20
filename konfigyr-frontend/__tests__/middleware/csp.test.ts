import { assert, beforeEach, describe, expect, test } from 'vitest';
import { NextRequest } from 'next/server';
import csp, { generateNonce, CSP_HEADER_NAME, NONCE_HEADER_NAME } from 'konfigyr/middleware/csp';

describe('middleware | csp', () => {
  let request: NextRequest;

  beforeEach(() => {
    request = new NextRequest('http:/localhost/test-uri');
  });

  test('should resolve CSP header value for production', () => {
    const value = csp(request, 'test-nonce');
    assert.isString(value, 'CSP header must be a string');

    const directives = value.split('; ');
    expect(directives).toHaveLength(14);

    expect(directives).toContain('base-uri \'self\'');
    expect(directives).toContain('default-src \'self\'');
    expect(directives).toContain('form-action \'self\'');
    expect(directives).toContain('frame-src \'self\'');
    expect(directives).toContain('connect-src \'self\'');
    expect(directives).toContain('manifest-src \'self\'');
    expect(directives).toContain('object-src \'none\'');
    expect(directives).toContain('frame-ancestors \'none\'');
    expect(directives).toContain('style-src-elem \'self\' \'unsafe-inline\'');
    expect(directives).toContain('script-src \'self\' \'nonce-test-nonce\' \'strict-dynamic\'');
    expect(directives).toContain('style-src \'self\' \'nonce-test-nonce\'');
    expect(directives).toContain('upgrade-insecure-requests ');
  });

  test('should add `Content-Security-Policy` and `X-Nonce` header to request ', () => {
    const value = csp(request);

    assert.deepEqual(request.headers.get(CSP_HEADER_NAME), value, 'Content-Security-Policy must match generated value');
    assert.isString(request.headers.get(NONCE_HEADER_NAME), 'X-Nonce header must be set and must be a string');
  });

  test('should generate unique nonce value', () => {
    const nonce = generateNonce();

    assert.isString(nonce, 'CSP nonce must be a string');
    assert.notEqual(nonce, generateNonce(), 'CSP nonce must be unique');
  });
});
