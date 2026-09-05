import {
  GlobeIcon,
  LockIcon,
} from 'lucide-react';
import { Badge } from '@konfigyr/components/ui/badge';
import { PrivateLabel, PublicLabel } from './messages';

import type { ComponentProps } from 'react';
import type { ArtifactVisibility } from '@konfigyr/hooks/artifactory/types';

export function ArtifactVisibilityBadge({ visibility, ...props }: {
  visibility: ArtifactVisibility;
} & Omit<ComponentProps<typeof Badge>, 'variant'>) {
  if (visibility === 'PUBLIC') {
    return (
      <Badge variant="success" {...props}>
        <GlobeIcon /> <PublicLabel/>
      </Badge>
    );
  }

  return (
    <Badge variant="secondary" {...props}>
      <LockIcon /> <PrivateLabel/>
    </Badge>
  );
}
