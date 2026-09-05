import { Bot, KeyRound, Workflow } from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import { Badge } from '@konfigyr/components/ui/badge';
import { cn } from '@konfigyr/components/utils';

import type { ComponentProps, ReactNode } from 'react';
import type { LucideComponent } from 'lucide-react';
import type { NamespaceApplicationType } from '@konfigyr/hooks/types';

interface ApplicationTypeConfig {
  icon: typeof LucideComponent;
  label: ReactNode;
  description: ReactNode;
  instructions: ReactNode;
}

const APPLICATION_TYPE_CONFIG: Record<NamespaceApplicationType, ApplicationTypeConfig> = {
  SERVICE_ACCOUNT: {
    icon: KeyRound,
    label: (
      <FormattedMessage
        defaultMessage="Service Account"
        description="Application type label for service account"
      />
    ),
    description: (
      <FormattedMessage
        defaultMessage="A long-lived OAuth client used by backend services. Authenticates with a client ID and secret using the client credentials flow."
        description="Application type description for service account"
      />
    ),
    instructions: (
      <FormattedMessage
        defaultMessage="Tokens are issued via the client credentials flow, scoped to the permissions assigned to this application. Grab the client ID and secret below and drop them into your OAuth2 config, your service is ready to go."
        description="Description shown in the details card for a Service Account application type"
      />
    ),
  },
  AGENT: {
    icon: Bot,
    label: (
      <FormattedMessage
        defaultMessage="AI Agent"
        description="Application type label for AI agent"
      />
    ),
    description: (
      <FormattedMessage
        defaultMessage="An OAuth client for AI assistants. Allows an agent to access the Konfigyr MCP server on behalf of a signed-in namespace member."
        description="Application type description for AI agent"
      />
    ),
    instructions: (
      <FormattedMessage
        defaultMessage="This agent authenticates on behalf of a signed-in namespace member using the authorization code flow. Wire it up with the credentials and redirect URIs below, and it'll only ever see what that member is allowed to see."
        description="Description shown in the details card for an AI Agent application type"
      />
    ),
  },
  WORKLOAD: {
    icon: Workflow,
    label: (
      <FormattedMessage
        defaultMessage="Workload Identity"
        description="Application type label for workload identity"
      />
    ),
    description: (
      <FormattedMessage
        defaultMessage="Secretless access for CI/CD pipelines. Exchanges a platform OIDC token (e.g. GitHub Actions) for a scoped API token, no long-lived credentials stored."
        description="Application type description for workload identity"
      />
    ),
    instructions: (
      <FormattedMessage
        defaultMessage="This workload identity accepts OIDC tokens from the configured issuer and exchanges them for scoped API tokens. No secrets to store, nothing to rotate, your pipeline just brings its own token and gets to work."
        description="Description shown in the details card for a Workload Identity application type"
      />
    ),
  },
};

export function ApplicationTypeIcon({ type, ...props }: { type: NamespaceApplicationType } & ComponentProps<typeof LucideComponent>) {
  const Icon = APPLICATION_TYPE_CONFIG[type].icon;

  return (
    <Icon {...props} />
  );
}

export function ApplicationTypeInstruction({ type }: { type: NamespaceApplicationType }) {
  return APPLICATION_TYPE_CONFIG[type].instructions;
}

/**
 * Renders the icon, label, and description for a given application type.
 * Used inside the type selector cards.
 */
export function ApplicationTypeInfo({ type, selected, className }: {
  type: NamespaceApplicationType;
  selected?: boolean;
  className?: string;
}) {
  const { icon: Icon, label, description } = APPLICATION_TYPE_CONFIG[type];

  return (
    <div className={cn('flex flex-col gap-2', className)}>
      <div className="flex items-center gap-2">
        <span className={cn('rounded-md p-1', selected ? 'bg-primary/10' : 'bg-muted')}>
          <Icon size="1.25rem" />
        </span>
        <span className="font-semibold">
          {label}
        </span>
      </div>
      <p className="text-sm text-muted-foreground">{description}</p>
    </div>
  );
}

/**
 * Renders the application type as a compact badge with icon and label.
 * Used in list items and card headers.
 */
export function ApplicationTypeBadge({ type, variant = 'default' }: {
  type: NamespaceApplicationType,
  variant?: 'default' | 'icon';
}) {
  const { icon: Icon, label } = APPLICATION_TYPE_CONFIG[type];
  return (
    <Badge variant="outline" className="gap-1 font-normal">
      {variant === 'icon' && (
        <Icon size="1rem" />
      )}

      {label}
    </Badge>
  );
}
