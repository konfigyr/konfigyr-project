import { createFileRoute } from '@tanstack/react-router';
import { AboutContent } from '@konfigyr/components/about/about-content';

export const Route = createFileRoute('/about')({
  component: AboutContent,
  head: () => ({
    meta: [{
      title: 'About | Konfigyr',
    }],
  }),
});
