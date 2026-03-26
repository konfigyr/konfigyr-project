import { Avatar, AvatarFallback, AvatarImage } from '@konfigyr/components/ui/avatar';
import { History } from 'lucide-react';
import { buttonVariants } from '@konfigyr/components/ui/button';
import { FormattedMessage } from 'react-intl';
import { RelativeDate } from '@konfigyr/components/messages';
import { Link } from '@tanstack/react-router';
import { useGetChangeHistory } from '@konfigyr/hooks';
import type { Namespace, Service } from '@konfigyr/hooks/namespace/types';
import type { ChangeHistory, Profile } from '@konfigyr/hooks/vault/types';

export type ChangeHistoryAlertProps = {
  namespace: Namespace,
  profile: Profile,
  service: Service
};

export type LatestChangeHistoryProps = {
  changeHistory: ChangeHistory,
  total: number,
} & ChangeHistoryAlertProps;

export function ChangeHistoryAlert({ namespace, service, profile }: ChangeHistoryAlertProps) {
  const { data: changeHistory } = useGetChangeHistory(namespace, service, profile, {
    size: 1,
  });

  return (
    <>
      { changeHistory?.data[0] && (
        <LatestChangeHistory
          changeHistory={changeHistory.data[0]}
          total={changeHistory.metadata.total}
          namespace={namespace}
          service={service}
          profile={profile}
        />
      )}
    </>
  );
}

function LatestChangeHistory({ changeHistory, total, namespace, service, profile }: LatestChangeHistoryProps) {
  return (
    <div className="flex gap-4 rounded-lg border py-2 px-4 text-sm">
      <div className="flex-1">
        <div className="flex items-center space-x-2">
          <Avatar>
            <AvatarImage
              src="https://github.com/shadcn.png"
              alt={changeHistory.appliedBy}
              className="grayscale"
            />
            <AvatarFallback>{changeHistory.appliedBy}</AvatarFallback>
          </Avatar>
          <span className="font-bold">{changeHistory.appliedBy}</span>
          <span className="truncate w-lg text-muted-foreground">
            {changeHistory.description}
          </span>
        </div>
      </div>

      <div className="flex items-center justify-end text-muted-foreground">
        <span className="truncate [direction:rtl] text-left w-16">
          {changeHistory.id}
        </span>
      </div>

      <div className="flex items-center justify-end text-muted-foreground">
        <RelativeDate value={changeHistory.appliedAt} />
      </div>

      <div className="flex items-center text-right">
        <Link
          to="/namespace/$namespace/services/$service/profiles/$profile/history"
          className={buttonVariants({ variant: 'outline', size: 'sm' })}
          params={{
            namespace: namespace.slug,
            service: service.slug,
            profile: profile.slug,
          }}
        >
          <History />
          <FormattedMessage
            defaultMessage="{amount} Commits"
            values={{ amount: total }}
            description="Label for the buttom with amount of commints."
          />
        </Link>
      </div>
    </div>
  );
}
