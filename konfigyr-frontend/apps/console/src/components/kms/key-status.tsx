import { FormattedMessage } from 'react-intl';
import {
  AlertTriangleIcon,
  CheckCircleIcon,
  ClockIcon,
  LoaderIcon,
  MinusCircleIcon,
  ShieldAlertIcon,
  Trash2Icon,
  XCircleIcon,
} from 'lucide-react';
import { SimpleAlert } from '@konfigyr/components/ui/alert';
import { Badge } from '@konfigyr/components/ui/badge';
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@konfigyr/components/ui/tooltip';

import type { ReactNode } from 'react';
import type { LucideComponent } from 'lucide-react';
import type { AlertProps } from '@konfigyr/components/ui/alert';
import type { BadgeProps } from '@konfigyr/components/ui/badge';
import type { KeyStatus } from '@konfigyr/hooks/kms/types';

interface KeyStatusAlertConfig {
  variant: AlertProps['variant'];
  Icon: typeof LucideComponent;
  title: ReactNode;
  description: ReactNode;
}

interface KeyStatusBadgeConfig {
  variant: BadgeProps['variant'];
  Icon: typeof LucideComponent;
  label: ReactNode;
  description: ReactNode;
}

const BADGE_STATUS_CONFIG: Record<KeyStatus, KeyStatusBadgeConfig> = {
  ENABLED: {
    variant: 'success',
    Icon: CheckCircleIcon,
    label: <FormattedMessage
      defaultMessage="Active"
      description="Label for the active KMS key status"
    />,
    description: <FormattedMessage
      defaultMessage="This key is active and being used for all new cryptographic operations."
      description="Description for the active KMS key status"
    />,
  },
  DISABLED: {
    variant: 'outline',
    Icon: MinusCircleIcon,
    label: <FormattedMessage
      defaultMessage="Disabled"
      description="Label for the disabled KMS key status"
    />,
    description: <FormattedMessage
      defaultMessage="This key is disabled and will not be used for any new operations. Existing data it protected remains readable."
      description="Description text for the disabled KMS key status"
    />,
  },
  INITIALIZING: {
    variant: 'info',
    Icon: LoaderIcon,
    label: <FormattedMessage
      defaultMessage="Setting up"
      description="Label for the initializing KMS key status"
    />,
    description: <FormattedMessage
      defaultMessage="Key material is being generated. The key will become active automatically once initialization is complete."
      description="Description text for the initializing KMS key status"
    />,
  },
  INITIALIZATION_FAILED: {
    variant: 'warning',
    Icon: XCircleIcon,
    label: <FormattedMessage
      defaultMessage="Setup failed"
      description="Label for the initialization failed KMS key status"
    />,
    description: <FormattedMessage
      defaultMessage="Key material could not be generated. The key is unusable. Delete it and create a new one to retry."
      description="Description text for the initialization failed KMS key status"
    />,
  },
  PENDING_DESTRUCTION: {
    variant: 'warning',
    Icon: ClockIcon,
    label: <FormattedMessage
      defaultMessage="Pending deletion"
      description="Label for the pending deletion KMS key status"
    />,
    description: <FormattedMessage
      defaultMessage="Destruction has been scheduled. The key is in its grace period and no operations are allowed. Restore it to cancel."
      description="Description text for the pending deletion KMS key status"
    />,
  },
  DESTRUCTION_FAILED: {
    variant: 'warning',
    Icon: AlertTriangleIcon,
    label: <FormattedMessage
      defaultMessage="Deletion failed"
      description="Label for the destruction failed KMS key status"
    />,
    description: <FormattedMessage
      defaultMessage="An error occurred while trying to destroy this key. The key material may still exist. Retry or contact support."
      description="Description text for the destruction failed KMS key status"
    />,
  },
  DESTROYED: {
    variant: 'destructive',
    Icon: Trash2Icon,
    label: <FormattedMessage
      defaultMessage="Destroyed"
      description="Label for the destroyed KMS key status"
    />,
    description: <FormattedMessage
      defaultMessage="This key has been permanently destroyed. Its material cannot be recovered and any data it protected is no longer accessible."
      description="Description text for the destroyed KMS key status"
    />,
  },
  COMPROMISED: {
    variant: 'destructive',
    Icon: ShieldAlertIcon,
    label: <FormattedMessage
      defaultMessage="Compromised"
      description="Label for the compromised KMS key status"
    />,
    description: <FormattedMessage
      defaultMessage="This key has been flagged as compromised and is disabled. Any data it protected should be considered at risk and re-encrypted immediately."
      description="Description text for the compromised KMS key status"
    />,
  },
};

const ALERT_STATUS_CONFIG: Record<string, KeyStatusAlertConfig | undefined> = {
  COMPROMISED: {
    variant: 'destructive',
    Icon: ShieldAlertIcon,
    title: <FormattedMessage
      defaultMessage="This keyset is compromised"
      description="Alert title for the compromised KMS key status"
    />,
    description: <FormattedMessage
      defaultMessage="Rotate immediately and re-protect any data it was used on."
      description="Alert description text for the compromised KMS key status"
    />,
  },
  PENDING_DESTRUCTION: {
    variant: 'default',
    Icon: ClockIcon,
    title: <FormattedMessage
      defaultMessage="This keyset is pending deletion"
      description="Alert title for the pending destruction KMS key status"
    />,
    description: <FormattedMessage
      defaultMessage="No cryptographic operations are allowed. You can still cancel deletion before the grace period ends."
      description="Alert description text for the pending destruction KMS key status"
    />,
  },
  DESTRUCTION_FAILED: {
    variant: 'default',
    Icon: AlertTriangleIcon,
    title: <FormattedMessage
      defaultMessage="Deletion failed"
      description="Alert title for the destruction failiure KMS key status"
    />,
    description: <FormattedMessage
      defaultMessage="An error occurred while trying to destroy this keyset. The key material may still exist. Retry or contact support."
      description="Alert description text for the destruction failiure KMS key status"
    />,
  },
  DISABLED: {
    variant: 'default',
    Icon: MinusCircleIcon,
    title: <FormattedMessage
      defaultMessage="This keyset is disabled"
      description="Alert title for the disabled KMS key status"
    />,
    description: <FormattedMessage
      defaultMessage="No cryptographic operations are allowed. Reactivate it to resume use."
      description="Alert description text for the disabled KMS key status"
    />,
  },
  DESTROYED: {
    variant: 'destructive',
    Icon: Trash2Icon,
    title: <FormattedMessage
      defaultMessage="This keyset has been permanently deleted"
      description="Alert title for the destroyed KMS key status"
    />,
    description: <FormattedMessage
      defaultMessage="The key material cannot be recovered and any data it protected is no longer accessible."
      description="Alert description text for the destroyed KMS key status"
    />,
  },
  INITIALIZATION_FAILED: {
    variant: 'default',
    Icon: AlertTriangleIcon,
    title: <FormattedMessage
      defaultMessage="Keyset initialization failed"
      description="Alert title for the initialization failiure KMS key status"
    />,
    description: <FormattedMessage
      defaultMessage="Key material could not be generated. The keyset is unusable. Delete it and create a new one to retry."
      description="Alert description text for the initialization failiure KMS key status"
    />,
  },
};

export function KeyStatusBadge({ status, delay = 300, ...props }: { status: KeyStatus, delay?: number } & Omit<BadgeProps, 'variant'>) {
  const { variant, Icon, label, description } = BADGE_STATUS_CONFIG[status];

  return (
    <Tooltip>
      <TooltipTrigger delay={delay}>
        <Badge variant={variant} {...props}>
          <Icon />
          {label}
        </Badge>
      </TooltipTrigger>
      <TooltipContent side="top">
        {description}
      </TooltipContent>
    </Tooltip>
  );
}

export function KeyStatusAlert({ status, ...props }: { status: KeyStatus } & Omit<AlertProps, 'variant'>) {
  const config = ALERT_STATUS_CONFIG[status];

  if (!config) {
    return null;
  }

  const { variant, Icon, title, description } = config;

  return (
    <SimpleAlert
      variant={variant}
      icon={<Icon className="size-4" />}
      title={title}
      description={description}
      {...props}
    />
  );
}
