import { HistoryIcon } from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import { Link } from '@tanstack/react-router';
import { useGetChangeHistory } from '@konfigyr/hooks';
import { RelativeDate } from '@konfigyr/components/messages';
import { buttonVariants } from '@konfigyr/components/ui/button';

import type { Namespace, Service } from '@konfigyr/hooks/namespace/types';
import type { ChangeHistory, Profile } from '@konfigyr/hooks/vault/types';

export type ChangeHistoryAlertProps = {
  namespace: Namespace,
  profile: Profile,
  service: Service
};

export type LatestChangeHistoryProps = {
  changeHistory: ChangeHistory,
} & ChangeHistoryAlertProps;

export function ChangeHistoryAlert({ namespace, service, profile }: ChangeHistoryAlertProps) {
  const { data: changeHistory } = useGetChangeHistory(namespace, service, profile, {
    size: 1,
  });

  return (
    <>
      {changeHistory?.data[0] && (
        <LatestChangeHistory
          changeHistory={changeHistory.data[0]}
          namespace={namespace}
          service={service}
          profile={profile}
        />
      )}
    </>
  );
}

function LatestChangeHistory({ changeHistory, namespace, service, profile }: LatestChangeHistoryProps) {
  return (
    <div
      role="log"
      aria-label={changeHistory.subject}
      className="flex gap-4 rounded-lg border items-center py-2 px-4 text-sm"
    >
      <p className="font-bold">
        {changeHistory.appliedBy}
      </p>

      <p className="flex-1 truncate w-lg text-muted-foreground">
        {changeHistory.subject}
      </p>

      <p className="text-muted-foreground text-left truncate w-24">
        {changeHistory.revision}
      </p>

      <p className="text-muted-foreground">
        <RelativeDate value={changeHistory.appliedAt} />
      </p>

      <Link
        to="/namespace/$namespace/services/$service/profiles/$profile/history"
        className={buttonVariants({ variant: 'outline', size: 'sm' })}
        params={{
          namespace: namespace.slug,
          service: service.slug,
          profile: profile.slug,
        }}
      >
        <HistoryIcon />
        <FormattedMessage
          defaultMessage="View history"
          description="Label for the view Vault change history button."
        />
      </Link>
    </div>
  );
}
