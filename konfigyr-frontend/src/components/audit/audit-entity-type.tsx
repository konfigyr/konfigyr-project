import { useIntl } from 'react-intl';
import {
  AppWindowMacIcon,
  FolderKanbanIcon,
  FolderKeyIcon,
  GitBranchIcon,
  Grid2x2XIcon,
} from 'lucide-react';

import type { LucideProps } from 'lucide-react';

export type AuditEntityType = string | ('namespace' | 'keyset' | 'service' | 'profile');

export function useAuditEntityTypeLabel(): (value: AuditEntityType) => string {
  const intl = useIntl();

  return (value: AuditEntityType)=> {
    switch (value) {
      case 'namespace':
        return intl.formatMessage({
          defaultMessage: 'Namespace',
          description: 'Label for the namespace audit entity type',
        });
      case 'keyset':
        return intl.formatMessage({
          defaultMessage: 'KMS Keyset',
          description: 'Label for the keyset audit entity type',
        });
      case 'service':
        return intl.formatMessage({
          defaultMessage: 'Service',
          description: 'Label for the service audit entity type',
        });
      case 'profile':
        return intl.formatMessage({
          defaultMessage: 'Service profile',
          description: 'Label for the profile audit entity type',
        });
      default:
        return value;
    }
  };
}

export function AuditEntityTypeIcon({ value, ...props }: { value: AuditEntityType } & LucideProps) {
  const entityTypeLabelFor = useAuditEntityTypeLabel();
  const label = entityTypeLabelFor(value);

  switch (value) {
    case 'namespace':
      return (
        <FolderKanbanIcon {...props} aria-label={label} />
      );
    case 'keyset':
      return (
        <FolderKeyIcon {...props} aria-label={label} />
      );
    case 'service':
      return (
        <AppWindowMacIcon {...props} aria-label={label} />
      );
    case 'profile':
      return (
        <GitBranchIcon {...props} aria-label={label} />
      );
    default:
      return (
        <Grid2x2XIcon {...props} aria-label={label} />
      );
  }
}

