import { createFileRoute } from '@tanstack/react-router';
import { GeneralErrorDetail, GeneralErrorTitle } from '@konfigyr/components/messages';
import {
  ErrorPage,
  ErrorPageFooter,
  ErrorPageHeader,
} from '@konfigyr/components/error/page';

export const Route = createFileRoute('/error')({
  component: RouteComponent,
});

function RouteComponent() {
  return (
    <ErrorPage>
      <ErrorPageHeader
        title={<GeneralErrorTitle />}
        description={<GeneralErrorDetail />}
      />
      <ErrorPageFooter />
    </ErrorPage>
  );
}
