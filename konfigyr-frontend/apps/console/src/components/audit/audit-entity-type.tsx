import { useIntl } from 'react-intl';
import {
  AppWindowMacIcon,
  ArrowLeftRightIcon,
  FolderKanbanIcon,
  FolderKeyIcon,
  GitBranchIcon,
  Grid2x2XIcon,
  MailPlusIcon,
  MonitorCloudIcon,
  ShieldCheckIcon,
  TagIcon,
  UserCircleIcon,
} from 'lucide-react';

import type { LucideProps } from 'lucide-react';

export type AuditEntityType = string | ('account' | 'namespace' | 'namespace-application' | 'namespace-trusted-issuer'
  | 'invitation' | 'keyset' | 'service' | 'profile' | 'artifact-version' | 'artifact-ownership-transfer');

export function useAuditEntityTypeLabel(): (value: AuditEntityType) => string {
  const intl = useIntl();

  return (value: AuditEntityType) => {
    switch (value) {
      case 'account':
        return intl.formatMessage({
          defaultMessage: 'Account',
          description: 'Label for the account audit entity type',
        });
      case 'namespace':
        return intl.formatMessage({
          defaultMessage: 'Namespace',
          description: 'Label for the namespace audit entity type',
        });
      case 'namespace-application':
        return intl.formatMessage({
          defaultMessage: 'Application',
          description: 'Label for the namespace application audit entity type',
        });
      case 'namespace-trusted-issuer':
        return intl.formatMessage({
          defaultMessage: 'Trusted issuer',
          description: 'Label for the namespace trusted issuer audit entity type',
        });
      case 'invitation':
        return intl.formatMessage({
          defaultMessage: 'Invitation',
          description: 'Label for the invitation audit entity type',
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
      case 'artifact-version':
        return intl.formatMessage({
          defaultMessage: 'Artifact version',
          description: 'Label for the artifact version audit entity type',
        });
      case 'artifact-ownership-transfer':
        return intl.formatMessage({
          defaultMessage: 'Ownership transfer',
          description: 'Label for the artifact ownership transfer audit entity type',
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
    case 'account':
      return (
        <UserCircleIcon {...props} aria-label={label} />
      );
    case 'namespace':
      return (
        <FolderKanbanIcon {...props} aria-label={label} />
      );
    case 'namespace-application':
      return (
        <MonitorCloudIcon {...props} aria-label={label} />
      );
    case 'namespace-trusted-issuer':
      return (
        <ShieldCheckIcon {...props} aria-label={label} />
      );
    case 'invitation':
      return (
        <MailPlusIcon {...props} aria-label={label} />
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
    case 'artifact-version':
      return (
        <TagIcon {...props} aria-label={label} />
      );
    case 'artifact-ownership-transfer':
      return (
        <ArrowLeftRightIcon {...props} aria-label={label} />
      );
    default:
      return (
        <Grid2x2XIcon {...props} aria-label={label} />
      );
  }
}

