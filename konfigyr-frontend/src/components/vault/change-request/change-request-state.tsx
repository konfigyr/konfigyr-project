import {
  GitMergeIcon,
  GitPullRequestClosedIcon,
  GitPullRequestIcon,
} from 'lucide-react';
import { ChangeRequestState } from '@konfigyr/hooks/vault/types';
import { Badge } from '@konfigyr/components/ui/badge';
import { ChangeRequestStateLabel } from './messages';

import type { ComponentProps } from 'react';

export function ChangeRequestStateBadge({ value }: { value: ChangeRequestState }) {
  return (
    <Badge variant="outline" size="lg" className="py-3 font-medium">
      <ChangeRequestStateIcon value={value} data-icon="inline-start" />
      <ChangeRequestStateLabel value={value} />
    </Badge>
  );
}

export function ChangeRequestStateIcon({ value, ...props }: { value: ChangeRequestState } & ComponentProps<'svg'> ) {
  switch (value) {
    case ChangeRequestState.OPEN:
      return (
        <GitPullRequestIcon className="text-emerald-600" {...props} />
      );
    case ChangeRequestState.MERGED:
      return (
        <GitMergeIcon className="text-blue-600" {...props} />
      );
    case ChangeRequestState.DISCARDED:
      return (
        <GitPullRequestClosedIcon className="text-destructive" {...props} />
      );
  }
}
