import { FormattedMessage } from 'react-intl';
import { cn } from '@konfigyr/components/utils';
import { Badge } from '@konfigyr/components/ui/badge';
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@konfigyr/components/ui/tooltip';

import type { ComponentProps, ReactNode } from 'react';
import type { PropertyDeprecation } from '@konfigyr/hooks/artifactory/types';

export function PropertyDeprecation({ deprecation }: { deprecation?: PropertyDeprecation }) {
  if (!deprecation) {
    return null;
  }

  return (
    <PropertyDeprecationTooltip deprecation={deprecation}>
      <PropertyDeprecationBadge />
    </PropertyDeprecationTooltip>
  );
}

export function PropertyDeprecationTooltip({ deprecation, children }: { deprecation: PropertyDeprecation, children: ReactNode }) {
  return (
    <Tooltip>
      <TooltipTrigger>
        {children}
      </TooltipTrigger>
      <TooltipContent className="grid max-w-full">
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
              tagName="b"
            />
            <pre>{deprecation.replacement}</pre>
          </>
        )}

      </TooltipContent>
    </Tooltip>
  );
}

export function PropertyDeprecationBadge({ className, ...props }: ComponentProps<typeof Badge>) {
  return (
    <Badge
      variant="outline"
      size="sm"
      className={cn('font-normal border-destructive/40 text-destructive', className)}
      {...props}
    >
      <FormattedMessage
        defaultMessage="deprecated"
        description="Label used for property deprecation state badge. Should trigger a tooltip with the deprecation reason or replacement."
      />
    </Badge>
  );
}
