import { createFileRoute } from '@tanstack/react-router';
import { Imprint } from '@konfigyr/components/legal/imprint';

export const Route = createFileRoute('/imprint')({
  component: Imprint,
  head: () => ({
    meta: [{
      title: 'Imprint | Konfigyr',
    }],
  }),
});
