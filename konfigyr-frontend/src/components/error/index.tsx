'use client';

import type { ReactElement } from 'react';
import { ServerCrash, SearchX } from 'lucide-react';
import { useTranslations } from 'next-intl';
import { HttpResponseError } from 'konfigyr/services/http';
import { Alert, AlertTitle, AlertDescription } from 'konfigyr/components/ui';

export type ErrorProps = {
  error: Error | unknown
};

const iconForStatusCode = (status: number): ReactElement => {
  switch (status) {
    case 404:
      return <SearchX />;
    default:
      return <ServerCrash />;
  }
};

export default function ErrorComponent({ error }: ErrorProps) {
  console.warn('Rendering error', error);
  console.trace(error);

  const t = useTranslations('errors');

  let title: string | undefined;
  let detail: string | undefined;
  let icon: ReactElement | undefined;

  if (error instanceof HttpResponseError) {
    title = error.problem.title;
    detail = error.problem.detail;
    icon = iconForStatusCode(error.status);
  } else if (error instanceof String) {
    detail = error as string;
  }

  if (!title) {
    title = t('title');
  }
  if (!detail) {
    detail = t('detail');
  }
  if (!icon) {
    icon = <ServerCrash />;
  }

  return (
    <Alert variant="destructive">
      {icon}
      <AlertTitle>{title}</AlertTitle>
      <AlertDescription>{detail}</AlertDescription>
    </Alert>
  );
}
