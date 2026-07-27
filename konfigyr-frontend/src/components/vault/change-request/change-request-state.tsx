import {
  GitMergeIcon,
  GitPullRequestClosedIcon,
  GitPullRequestIcon,
} from 'lucide-react';
import { ChangeRequestState } from '@konfigyr/hooks/vault/types';
import { Badge } from '@konfigyr/components/ui/badge';
import { cn } from '@konfigyr/components/utils';
import { ChangeRequestStateLabel } from './messages';

import type { ComponentProps } from 'react';

export function ChangeRequestStateBadge({ value }: { value: ChangeRequestState }) {
  return (
    <Badge variant="outline" size="lg">
      <ChangeRequestStateIcon value={value} data-icon="inline-start" className="size-4!" />
      <ChangeRequestStateLabel value={value} />
    </Badge>
  );
}

export function ChangeRequestStateIcon({ value, className, ...props }: { value: ChangeRequestState } & ComponentProps<'svg'> ) {
  switch (value) {
    case ChangeRequestState.OPEN:
      return (
        <GitPullRequestIcon className={cn('text-success', className)} {...props} />
      );
    case ChangeRequestState.MERGED:
      return (
        <GitMergeIcon className={cn('text-info', className)} {...props} />
      );
    case ChangeRequestState.DISCARDED:
      return (
        <GitPullRequestClosedIcon className={cn('text-destructive', className)} {...props} />
      );
  }
}
