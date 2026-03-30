import { FormattedMessage } from 'react-intl';
import { cn } from '@konfigyr/components/utils';

import type { ComponentProps } from 'react';

export function PropertyDefaultValue({
  variant = 'default',
  value,
  className,
  ...props
}: {
  variant: 'labeled' | 'default';
  value?: string;
} & ComponentProps<'span'>) {
  return (
    <span
      data-slot="artifactory-property-default-value"
      className={cn(
        'text-xs text-muted-foreground',
        variant === 'labeled' && 'inline-flex items-center gap-1',
        className,
      )}
      {...props}
    >
      {variant === 'labeled' && (
        <FormattedMessage
          defaultMessage="Default value:"
          description="Label for the default value of a property"
        />
      )}
      <span className="font-mono font-medium text-foreground">
        {value || 'N/A'}
      </span>
    </span>
  );
}
