import { FormattedMessage } from 'react-intl';
import { MessageCircleWarningIcon } from 'lucide-react';
import { cn } from '@konfigyr/components/utils';
import {
  Alert,
  AlertDescription,
  AlertTitle,
} from '@konfigyr/components/ui/alert';
import { Badge } from '@konfigyr/components/ui/badge';
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@konfigyr/components/ui/tooltip';

import type { ComponentProps, ReactNode } from 'react';
import type { PropertyDeprecation } from '@konfigyr/hooks/artifactory/types';

export function PropertyDeprecation({ deprecation, ...props }: { deprecation?: PropertyDeprecation } & ComponentProps<typeof Badge>) {
  if (!deprecation) {
    return null;
  }

  return (
    <PropertyDeprecationTooltip deprecation={deprecation}>
      <PropertyDeprecationBadge {...props}/>
    </PropertyDeprecationTooltip>
  );
}

function PropertyDeprecationContent({ deprecation }: { deprecation: PropertyDeprecation }) {
  return (
    <div className="grid max-w-full">
      <p className="leading-snug mb-1">
        {deprecation.reason ? deprecation.reason : (
          <FormattedMessage
            defaultMessage="No deprecation reason specified."
            description="Tooltip message displayed when property deprecation reason is not specified."
            tagName="i"
          />
        )}
      </p>

      {deprecation.replacement && (
        <>
          <FormattedMessage
            defaultMessage="Replaced by:"
            description="Label used in the property deprecation tooltip that shows the property replacement."
            tagName="strong"
          />
          <pre>{deprecation.replacement}</pre>
        </>
      )}
    </div>
  );
}

export function PropertyDeprecationTooltip({ deprecation, children }: { deprecation: PropertyDeprecation, children: ReactNode }) {
  return (
    <Tooltip>
      <TooltipTrigger>
        {children}
      </TooltipTrigger>
      <TooltipContent>
        <PropertyDeprecationContent deprecation={deprecation} />
      </TooltipContent>
    </Tooltip>
  );
}

export function PropertyDeprecationAlert({ deprecation, className }: { deprecation?: PropertyDeprecation, className?: string }) {
  if (!deprecation) {
    return null;
  }

  return (
    <Alert className={className}>
      <MessageCircleWarningIcon />
      <AlertTitle>
        <FormattedMessage
          defaultMessage="Deprecated"
          description="Title used for property deprecation alert."
        />
      </AlertTitle>
      <AlertDescription>
        <PropertyDeprecationContent deprecation={deprecation} />
      </AlertDescription>
    </Alert>
  );
}

export function PropertyDeprecationBadge({ className, ...props }: ComponentProps<typeof Badge>) {
  return (
    <Badge
      variant="outline"
      size="sm"
      className={cn('font-normal border-destructive/40 text-destructive!', className)}
      {...props}
    >
      <FormattedMessage
        defaultMessage="deprecated"
        description="Label used for property deprecation state badge. Should trigger a tooltip with the deprecation reason or replacement."
      />
    </Badge>
  );
}
