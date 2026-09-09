import { createFileRoute } from '@tanstack/react-router';
import { PrivacyPolicy } from '@konfigyr/components/legal/privacy-policy';

export const Route = createFileRoute('/privacy')({
  component: PrivacyPolicy,
  head: () => ({
    meta: [{
      title: 'Privacy Policy | Konfigyr',
    }],
  }),
});
