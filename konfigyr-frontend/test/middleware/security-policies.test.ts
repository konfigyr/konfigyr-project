import { afterEach, assert, describe, expect, test, vi } from 'vitest';
import {
  generateContentSecurityPolicies,
  securityPoliciesMiddleware,
} from '@konfigyr/middleware/security-policies';

const setResponseHeaders = vi.hoisted(() => vi.fn());

vi.mock('@tanstack/react-start/server', () => ({
  setResponseHeaders: setResponseHeaders,
}));

describe('middleware | security-policies', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  test('should generate security headers', async () => {
    const next = vi.fn();

    const middleware = securityPoliciesMiddleware();

    // @ts-expect-error: the type does not define the `server` method
    await middleware.options.server({
      context: vi.fn(),
      request: vi.fn(),
      next,
    } as any);

    expect(next).toHaveBeenCalled();
    expect(setResponseHeaders).toHaveBeenCalled();
  });

  test('should generate CSP policies for production', () => {
    const value = generateContentSecurityPolicies();
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
    expect(directives).toContain('script-src \'self\' \'strict-dynamic\'');
    expect(directives).toContain('upgrade-insecure-requests ');
  });

  test('should generate CSP policies for development', () => {
    const value = generateContentSecurityPolicies('development');
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
    expect(directives).toContain('script-src \'self\' \'strict-dynamic\' \'unsafe-eval\'');
    expect(directives).toContain('style-src \'self\'');
    expect(directives).toContain('upgrade-insecure-requests ');
  });
});
