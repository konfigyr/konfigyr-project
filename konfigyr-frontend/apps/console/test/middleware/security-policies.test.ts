import { afterEach, assert, describe, expect, test, vi } from 'vitest';
import {
  generateContentSecurityPolicies,
  securityPoliciesMiddleware,
} from '@konfigyr/middleware/security-policies';

const getRequest = vi.hoisted(() => vi.fn(() => new Object()));
const setResponseHeaders = vi.hoisted(() => vi.fn());

vi.mock('@tanstack/react-start/server', () => ({
  getRequest, setResponseHeaders,
}));

const generateNonce = vi.hoisted(() => vi.fn());
const setNonce = vi.hoisted(() => vi.fn());

vi.mock('@konfigyr/lib/nonces', () => ({
  generateNonce, setNonce,
}));

describe('middleware | security-policies', () => {
  afterEach(() => {
    vi.unstubAllEnvs();
    vi.restoreAllMocks();
  });

  test('should generate security headers', async () => {
    vi.stubEnv('NODE_ENV', 'production');
    vi.stubEnv('KONFIGYR_SSL_ENABLED', 'true');

    generateNonce.mockReturnValue('A1JApkBP8Sv7VEbSDuDsuA==');

    const next = vi.fn();
    const middleware = securityPoliciesMiddleware();

    // @ts-expect-error: the type does not define the `server` method
    await middleware.options.server({
      context: vi.fn(),
      request: vi.fn(),
      next,
    } as any);

    expect(next).toHaveBeenCalled();
    expect(setNonce).toHaveBeenCalledExactlyOnceWith('A1JApkBP8Sv7VEbSDuDsuA==');
    expect(setResponseHeaders).toHaveBeenCalledExactlyOnceWith(new Headers({
      'Content-Security-Policy': 'base-uri \'self\'; default-src \'self\'; frame-ancestors \'none\'; ' +
        'font-src \'self\' data:; form-action \'self\'; frame-src \'self\'; connect-src \'self\'; ' +
        'img-src \'self\' blob: data: https:; manifest-src \'self\'; object-src \'none\'; ' +
        'script-src \'self\' \'nonce-A1JApkBP8Sv7VEbSDuDsuA==\' \'strict-dynamic\'; ' +
        'style-src \'self\' \'unsafe-inline\'; style-src-elem \'self\' \'unsafe-inline\'; ' +
        'upgrade-insecure-requests ',
      'Cross-Origin-Opener-Policy': 'same-origin',
      'Cross-Origin-Resource-Policy': 'cross-origin',
      'Permissions-Policy': 'geolocation=(), camera=(), microphone=()',
      'Referrer-Policy': 'strict-origin-when-cross-origin',
      'X-Frame-Options': 'DENY',
      'X-Content-Type-Options': 'nosniff',
    }));
  });

  test('should generate security headers for development environment', async () => {
    vi.stubEnv('NODE_ENV', 'development');

    const next = vi.fn();
    const middleware = securityPoliciesMiddleware();

    // @ts-expect-error: the type does not define the `server` method
    await middleware.options.server({
      context: vi.fn(),
      request: vi.fn(),
      next,
    } as any);

    expect(next).toHaveBeenCalled();
    expect(generateNonce).not.toHaveBeenCalled();
    expect(setNonce).not.toHaveBeenCalled();
    expect(setResponseHeaders).toHaveBeenCalledExactlyOnceWith(new Headers({
      'Content-Security-Policy': 'base-uri \'self\'; default-src \'self\'; frame-ancestors \'none\'; ' +
        'font-src \'self\' data:; form-action \'self\'; frame-src \'self\'; connect-src \'self\'; ' +
        'img-src \'self\' blob: data: https:; manifest-src \'self\'; object-src \'none\'; ' +
        'script-src \'self\' \'unsafe-inline\' \'unsafe-eval\'; style-src \'self\' \'unsafe-inline\'; ' +
        'style-src-elem \'self\' \'unsafe-inline\'',
      'Cross-Origin-Opener-Policy': 'same-origin',
      'Cross-Origin-Resource-Policy': 'cross-origin',
      'Permissions-Policy': 'geolocation=(), camera=(), microphone=()',
      'Referrer-Policy': 'strict-origin-when-cross-origin',
      'X-Frame-Options': 'DENY',
      'X-Content-Type-Options': 'nosniff',
    }));
  });

  test('should generate CSP policies for production', () => {
    vi.stubEnv('KONFIGYR_SSL_ENABLED', undefined);

    const value = generateContentSecurityPolicies('production', 'dI6DYOF8Cg08kSmUygHRVw==');
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
    expect(directives).toContain('style-src-elem \'self\' \'unsafe-inline\'');
    expect(directives).toContain('script-src \'self\' \'nonce-dI6DYOF8Cg08kSmUygHRVw==\' \'strict-dynamic\'');
    expect(directives).toContain('upgrade-insecure-requests ');
  });

  test('should generate CSP policies for development', () => {
    vi.stubEnv('KONFIGYR_SSL_ENABLED', undefined);

    const value = generateContentSecurityPolicies('development');
    assert.isString(value, 'CSP header must be a string');

    const directives = value.split('; ');
    expect(directives).toHaveLength(13);

    expect(directives).toContain('base-uri \'self\'');
    expect(directives).toContain('default-src \'self\'');
    expect(directives).toContain('font-src \'self\' data:');
    expect(directives).toContain('form-action \'self\'');
    expect(directives).toContain('frame-src \'self\'');
    expect(directives).toContain('connect-src \'self\'');
    expect(directives).toContain('manifest-src \'self\'');
    expect(directives).toContain('object-src \'none\'');
    expect(directives).toContain('frame-ancestors \'none\'');
    expect(directives).toContain('style-src-elem \'self\' \'unsafe-inline\'');
    expect(directives).toContain('script-src \'self\' \'unsafe-inline\' \'unsafe-eval\'');
    expect(directives).toContain('style-src \'self\' \'unsafe-inline\'');
  });

  test('should generate CSP policies for development where SSL is enabled', () => {
    vi.stubEnv('KONFIGYR_SSL_ENABLED', 'true');

    const value = generateContentSecurityPolicies('development', 'ignored-nonce');
    assert.isString(value, 'CSP header must be a string');

    const directives = value.split('; ');
    expect(directives).toHaveLength(14);

    expect(directives).toContain('base-uri \'self\'');
    expect(directives).toContain('default-src \'self\'');
    expect(directives).toContain('font-src \'self\' data:');
    expect(directives).toContain('form-action \'self\'');
    expect(directives).toContain('frame-src \'self\'');
    expect(directives).toContain('connect-src \'self\'');
    expect(directives).toContain('manifest-src \'self\'');
    expect(directives).toContain('object-src \'none\'');
    expect(directives).toContain('frame-ancestors \'none\'');
    expect(directives).toContain('style-src-elem \'self\' \'unsafe-inline\'');
    expect(directives).toContain('script-src \'self\' \'unsafe-inline\' \'unsafe-eval\'');
    expect(directives).toContain('style-src \'self\' \'unsafe-inline\'');
    expect(directives).toContain('upgrade-insecure-requests ');
  });

  test('should generate CSP policies for production where SSL is disabled and nonce not supplied', () => {
    vi.stubEnv('KONFIGYR_SSL_ENABLED', 'false');

    const value = generateContentSecurityPolicies('production');
    assert.isString(value, 'CSP header must be a string');

    const directives = value.split('; ');
    expect(directives).toHaveLength(13);

    expect(directives).toContain('base-uri \'self\'');
    expect(directives).toContain('default-src \'self\'');
    expect(directives).toContain('font-src \'self\' data:');
    expect(directives).toContain('form-action \'self\'');
    expect(directives).toContain('frame-src \'self\'');
    expect(directives).toContain('connect-src \'self\'');
    expect(directives).toContain('manifest-src \'self\'');
    expect(directives).toContain('object-src \'none\'');
    expect(directives).toContain('frame-ancestors \'none\'');
    expect(directives).toContain('style-src \'self\' \'unsafe-inline\'');
    expect(directives).toContain('style-src-elem \'self\' \'unsafe-inline\'');
    expect(directives).toContain('script-src \'self\'');
  });
});
