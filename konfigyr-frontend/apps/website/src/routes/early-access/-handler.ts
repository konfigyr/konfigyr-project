import { z } from 'zod';
import ky, { isHTTPError } from 'ky';
import { log } from '@konfigyr/lib/log';

export const EarlyAccessSubmissionSchema = z.object({
  name: z.string().min(1),
  email: z.string().email(),
  company: z.string().min(1),
  teamSize: z.string().min(1),
  serviceCount: z.string().min(1),
  currentTooling: z.string().optional(),
  notes: z.string().optional(),
  /** Honeypot — must stay empty; real visitors never see or fill this field. */
  hp_field: z.string().optional(),
});

export type EarlyAccessSubmission = z.infer<typeof EarlyAccessSubmissionSchema>;

export interface SubmissionResult {
  ok: boolean;
  status?: number;
  detail?: string;
}

/**
 * Submits an early access request to HubSpot's Forms API.
 *
 * A filled honeypot field is treated as a bot: the caller is told submission
 * succeeded, but nothing is sent to HubSpot. When HUBSPOT_MOCK is set, the
 * submission is logged instead of calling the real HubSpot endpoint, so the
 * form can be built and tested before the HubSpot portal setup lands.
 */
export async function submitEarlyAccessRequest(submission: EarlyAccessSubmission): Promise<SubmissionResult> {
  if (submission.hp_field) {
    return { ok: true };
  }

  const { HUBSPOT_ACCESS_TOKEN, HUBSPOT_PORTAL_ID, HUBSPOT_FORM_GUID, HUBSPOT_MOCK } = process.env;

  if (HUBSPOT_MOCK === 'true') {
    log.info('early-access.mock-submit', { ...submission });
    return { ok: true };
  }

  if (!HUBSPOT_ACCESS_TOKEN || !HUBSPOT_PORTAL_ID || !HUBSPOT_FORM_GUID) {
    log.error('early-access.missing-config', {});
    return { ok: false, detail: 'Server is misconfigured: missing HubSpot credentials.' };
  }

  const [firstname, ...rest] = submission.name.trim().split(/\s+/);
  const lastname = rest.join(' ') || firstname;

  try {
    await ky.post(
      `https://api.hsforms.com/submissions/v3/integration/secure/submit/${HUBSPOT_PORTAL_ID}/${HUBSPOT_FORM_GUID}`,
      {
        headers: { Authorization: `Bearer ${HUBSPOT_ACCESS_TOKEN}` },
        json: {
          fields: [
            { name: 'firstname', value: firstname },
            { name: 'lastname', value: lastname },
            { name: 'email', value: submission.email },
            { name: 'company', value: submission.company },
            { name: 'team_size', value: submission.teamSize },
            { name: 'spring_boot_service_count', value: submission.serviceCount },
            ...(submission.currentTooling ? [{ name: 'current_tooling', value: submission.currentTooling }] : []),
            ...(submission.notes ? [{ name: 'message', value: submission.notes }] : []),
          ],
        },
        retry: 0,
      },
    );

    return { ok: true };
  } catch (error) {
    const status = isHTTPError(error) ? error.response.status : undefined;

    log.error('early-access.submit-failed', { email: submission.email, company: submission.company, status });

    return { ok: false, status, detail: 'Something went wrong submitting your request.' };
  }
}
