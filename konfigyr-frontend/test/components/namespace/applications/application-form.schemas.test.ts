import { describe, expect, test } from 'vitest';
import {
  createApplicationSchema,
  issuerUriSchema,
  redirectUriItemSchema,
  subjectPatternSchema,
} from '@konfigyr/components/namespace/applications/application-form';

const validFormBase = {
  name: 'my app',
  expiresAt: '',
  scopes: [],
  redirectUris: [],
  issuerUri: '',
  subjectPattern: '',
};

describe('redirectUriItemSchema', () => {
  test('rejects an empty string', () => {
    expect(redirectUriItemSchema.safeParse('').success).toBe(false);
  });

  test('rejects a whitespace-only string', () => {
    expect(redirectUriItemSchema.safeParse('   ').success).toBe(false);
  });

  test('rejects a string that is not a URL', () => {
    expect(redirectUriItemSchema.safeParse('not-a-url').success).toBe(false);
  });

  test('rejects a non-http/https scheme', () => {
    const result = redirectUriItemSchema.safeParse('ftp://example.com/callback');
    expect(result.success).toBe(false);
  });

  test('rejects http:// for non-loopback hosts', () => {
    const result = redirectUriItemSchema.safeParse('http://example.com/callback');
    expect(result.success).toBe(false);
    expect(result.error?.issues[0].message).toMatch(/loopback/i);
  });

  test('accepts http://127.0.0.1', () => {
    expect(redirectUriItemSchema.safeParse('http://127.0.0.1/callback').success).toBe(true);
  });

  test('accepts http://localhost', () => {
    expect(redirectUriItemSchema.safeParse('http://localhost/callback').success).toBe(true);
  });

  test('accepts http://[::1]', () => {
    expect(redirectUriItemSchema.safeParse('http://[::1]/callback').success).toBe(true);
  });

  test('accepts https:// for any host', () => {
    expect(redirectUriItemSchema.safeParse('https://example.com/callback').success).toBe(true);
  });

  test('accepts https:// without a path', () => {
    expect(redirectUriItemSchema.safeParse('https://example.com').success).toBe(true);
  });
});

describe('issuerUriSchema', () => {
  test('accepts an empty string', () => {
    expect(issuerUriSchema.safeParse('').success).toBe(true);
  });

  test('rejects http:// scheme', () => {
    const result = issuerUriSchema.safeParse('http://token.example.com');
    expect(result.success).toBe(false);
    expect(result.error?.issues[0].message).toMatch(/https/i);
  });

  test('rejects a string that is not a URL', () => {
    expect(issuerUriSchema.safeParse('not-a-url').success).toBe(false);
  });

  test('accepts a valid https:// URL', () => {
    expect(issuerUriSchema.safeParse('https://token.actions.githubusercontent.com').success).toBe(true);
  });
});

describe('subjectPatternSchema', () => {
  test('accepts an empty string', () => {
    expect(subjectPatternSchema.safeParse('').success).toBe(true);
  });

  test('rejects a whitespace-only string', () => {
    expect(subjectPatternSchema.safeParse('   ').success).toBe(false);
  });

  test('accepts a valid pattern', () => {
    expect(subjectPatternSchema.safeParse('repo:owner/name:ref:refs/heads/main').success).toBe(true);
  });
});

describe('createApplicationSchema', () => {
  describe('SERVICE_ACCOUNT', () => {
    const schema = createApplicationSchema('SERVICE_ACCOUNT');

    test('passes with minimal valid data', () => {
      expect(schema.safeParse({ ...validFormBase, name: 'my service' }).success).toBe(true);
    });

    test('passes regardless of redirectUris and issuerUri values', () => {
      expect(schema.safeParse({
        ...validFormBase,
        name: 'my service',
        redirectUris: [],
        issuerUri: '',
      }).success).toBe(true);
    });

    test('rejects a name shorter than 3 characters', () => {
      expect(schema.safeParse({ ...validFormBase, name: 'ab' }).success).toBe(false);
    });

    test('rejects a name longer than 30 characters', () => {
      expect(schema.safeParse({ ...validFormBase, name: 'a'.repeat(31) }).success).toBe(false);
    });

    test('rejects an invalid expiration date string', () => {
      expect(schema.safeParse({ ...validFormBase, name: 'my service', expiresAt: 'not-a-date' }).success).toBe(false);
    });

    test('accepts a valid expiration date', () => {
      expect(schema.safeParse({ ...validFormBase, name: 'my service', expiresAt: '2026-12-31' }).success).toBe(true);
    });
  });

  describe('AGENT', () => {
    const schema = createApplicationSchema('AGENT');

    test('fails when redirectUris is empty', () => {
      const result = schema.safeParse({ ...validFormBase, name: 'my agent', redirectUris: [] });
      expect(result.success).toBe(false);
      const issue = result.error?.issues.find(i => i.path[0] === 'redirectUris');
      expect(issue?.message).toMatch(/at least one/i);
    });

    test('passes with one valid redirect URI', () => {
      expect(schema.safeParse({
        ...validFormBase,
        name: 'my agent',
        redirectUris: ['https://example.com/callback'],
      }).success).toBe(true);
    });

    test('passes with multiple distinct redirect URIs', () => {
      expect(schema.safeParse({
        ...validFormBase,
        name: 'my agent',
        redirectUris: ['https://example.com/callback', 'https://other.example.com/callback'],
      }).success).toBe(true);
    });

    test('fails when redirect URIs contain duplicates', () => {
      const result = schema.safeParse({
        ...validFormBase,
        name: 'my agent',
        redirectUris: ['https://example.com/callback', 'https://example.com/callback'],
      });
      expect(result.success).toBe(false);
      const issue = result.error?.issues.find(i => i.path[0] === 'redirectUris');
      expect(issue?.message).toMatch(/unique/i);
    });

    test('does not require issuerUri', () => {
      expect(schema.safeParse({
        ...validFormBase,
        name: 'my agent',
        redirectUris: ['https://example.com/callback'],
        issuerUri: '',
      }).success).toBe(true);
    });
  });

  describe('WORKLOAD', () => {
    const schema = createApplicationSchema('WORKLOAD');

    test('fails when issuerUri is empty', () => {
      const result = schema.safeParse({ ...validFormBase, name: 'my workload', issuerUri: '' });
      expect(result.success).toBe(false);
      const issue = result.error?.issues.find(i => i.path[0] === 'issuerUri');
      expect(issue?.message).toMatch(/required/i);
    });

    test('fails when issuerUri uses http://', () => {
      const result = schema.safeParse({
        ...validFormBase,
        name: 'my workload',
        issuerUri: 'http://token.example.com',
      });
      expect(result.success).toBe(false);
      const issue = result.error?.issues.find(i => i.path[0] === 'issuerUri');
      expect(issue?.message).toMatch(/https/i);
    });

    test('fails when issuerUri is not a valid URL', () => {
      const result = schema.safeParse({
        ...validFormBase,
        name: 'my workload',
        issuerUri: 'not-a-url',
      });
      expect(result.success).toBe(false);
    });

    test('passes with a valid https:// issuerUri', () => {
      expect(schema.safeParse({
        ...validFormBase,
        name: 'my workload',
        issuerUri: 'https://token.actions.githubusercontent.com',
      }).success).toBe(true);
    });

    test('passes with a valid issuerUri and no subjectPattern', () => {
      expect(schema.safeParse({
        ...validFormBase,
        name: 'my workload',
        issuerUri: 'https://token.actions.githubusercontent.com',
        subjectPattern: '',
      }).success).toBe(true);
    });

    test('passes with a valid issuerUri and a valid subjectPattern', () => {
      expect(schema.safeParse({
        ...validFormBase,
        name: 'my workload',
        issuerUri: 'https://token.actions.githubusercontent.com',
        subjectPattern: 'repo:owner/name:ref:refs/heads/main',
      }).success).toBe(true);
    });

    test('fails when subjectPattern is whitespace-only', () => {
      const result = schema.safeParse({
        ...validFormBase,
        name: 'my workload',
        issuerUri: 'https://token.actions.githubusercontent.com',
        subjectPattern: '   ',
      });
      expect(result.success).toBe(false);
      const issue = result.error?.issues.find(i => i.path[0] === 'subjectPattern');
      expect(issue?.message).toMatch(/blank/i);
    });

    test('does not require redirectUris', () => {
      expect(schema.safeParse({
        ...validFormBase,
        name: 'my workload',
        issuerUri: 'https://token.actions.githubusercontent.com',
        redirectUris: [],
      }).success).toBe(true);
    });
  });
});
