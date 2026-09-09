import { afterEach, describe, expect, test, vi } from 'vitest';
import { HttpResponse, http } from 'msw';
import { server } from '@konfigyr/test/helpers/server';
import { submitEarlyAccessRequest } from '@konfigyr/routes/early-access/-handler';

import type { EarlyAccessSubmission } from '@konfigyr/routes/early-access/-handler';

const submission: EarlyAccessSubmission = {
  name: 'Ada Lovelace',
  email: 'ada@example.com',
  company: 'Analytical Engines Inc.',
  teamSize: '11-50',
  serviceCount: '5-10',
};

describe('routes | early-access | handler', () => {
  afterEach(() => {
    vi.unstubAllEnvs();
  });

  test('short-circuits and reports success without submitting when the honeypot is filled', async () => {
    const result = await submitEarlyAccessRequest({ ...submission, hp_field: 'i am a bot' });

    expect(result).toStrictEqual({ ok: true });
  });

  test('logs and returns success without calling HubSpot when HUBSPOT_MOCK is set', async () => {
    vi.stubEnv('HUBSPOT_MOCK', 'true');

    const result = await submitEarlyAccessRequest(submission);

    expect(result).toStrictEqual({ ok: true });
  });

  test('reports a misconfiguration when HubSpot credentials are missing', async () => {
    vi.stubEnv('HUBSPOT_MOCK', 'false');
    vi.stubEnv('HUBSPOT_ACCESS_TOKEN', '');
    vi.stubEnv('HUBSPOT_PORTAL_ID', '');
    vi.stubEnv('HUBSPOT_FORM_GUID', '');

    const result = await submitEarlyAccessRequest(submission);

    expect(result.ok).toBe(false);
    expect(result.detail).toMatch(/misconfigured/);
  });

  test('submits to the HubSpot Forms API and reports success', async () => {
    vi.stubEnv('HUBSPOT_MOCK', 'false');
    vi.stubEnv('HUBSPOT_ACCESS_TOKEN', 'token');
    vi.stubEnv('HUBSPOT_PORTAL_ID', 'portal');
    vi.stubEnv('HUBSPOT_FORM_GUID', 'form');

    const result = await submitEarlyAccessRequest(submission);

    expect(result).toStrictEqual({ ok: true });
  });

  test('reports a failure when the HubSpot request fails', async () => {
    vi.stubEnv('HUBSPOT_MOCK', 'false');
    vi.stubEnv('HUBSPOT_ACCESS_TOKEN', 'token');
    vi.stubEnv('HUBSPOT_PORTAL_ID', 'portal');
    vi.stubEnv('HUBSPOT_FORM_GUID', 'form');

    server.use(
      http.post('https://api.hsforms.com/submissions/v3/integration/secure/submit/:portalId/:formGuid', () => (
        HttpResponse.json({ message: 'invalid token' }, { status: 401 })
      )),
    );

    const result = await submitEarlyAccessRequest(submission);

    expect(result.ok).toBe(false);
    expect(result.status).toBe(401);
  });
});
