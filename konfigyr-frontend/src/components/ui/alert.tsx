import * as React from 'react';
import { cva } from 'class-variance-authority';
import { cn } from '@konfigyr/components/utils';

import type { VariantProps } from 'class-variance-authority';

const alertVariants = cva(
  'group/alert relative grid w-full gap-0.5 rounded-lg border px-2.5 py-2 text-left text-sm has-data-[slot=alert-action]:relative has-data-[slot=alert-action]:pr-18 has-[>svg]:grid-cols-[auto_1fr] has-[>svg]:gap-x-2 *:[svg]:row-span-2 *:[svg]:translate-y-0.5 *:[svg]:text-current *:[svg:not([class*="size-"])]:size-4',
  {
    variants: {
      variant: {
        default: 'bg-card text-card-foreground',
        destructive:
          'bg-card text-destructive *:data-[slot=alert-description]:text-destructive/90 *:[svg]:text-current',
      },
    },
    defaultVariants: {
      variant: 'default',
    },
  },
);

export type AlertProps = React.ComponentProps<'div'> & VariantProps<typeof alertVariants>;

export function Alert({ className, variant, ...props }: AlertProps) {
  return (
    <div
      data-slot="alert"
      role="alert"
      className={cn(alertVariants({ variant }), className)}
      {...props}
    />
  );
}

export function AlertTitle({ className, ...props }: React.ComponentProps<'div'>) {
  return (
    <div
      data-slot="alert-title"
      className={cn(
        'font-heading font-medium group-has-[>svg]/alert:col-start-2 [&_a]:underline [&_a]:underline-offset-3 [&_a]:hover:text-foreground',
        className,
      )}
      {...props}
    />
  );
}

export function AlertDescription({ className, ...props }: React.ComponentProps<'div'>) {
  return (
    <div
      data-slot="alert-description"
      className={cn(
        'text-sm text-balance text-muted-foreground md:text-pretty [&_a]:underline [&_a]:underline-offset-3 [&_a]:hover:text-foreground [&_p:not(:last-child)]:mb-4',
        className,
      )}
      {...props}
    />
  );
}

export function AlertAction({ className, ...props }: React.ComponentProps<'div'>) {
  return (
    <div
      data-slot="alert-action"
      className={cn('absolute top-2 right-2', className)}
      {...props}
    />
  );
}

export function SimpleAlert({ icon, title, description, ...props }: {
  icon?: React.ReactNode,
  title?: React.ReactNode,
  description?: React.ReactNode,
} & Omit<AlertProps, 'title'>) {
  const id = React.useId();
  const titleId = `${id}-alert-title`;
  const descriptionId = `${id}-alert-description`;

  return (
    <Alert {...props} aria-labelledby={titleId} aria-describedby={descriptionId}>
      {icon}
      <AlertTitle id={titleId}>{title}</AlertTitle>
      <AlertDescription id={descriptionId}>{description}</AlertDescription>
    </Alert>
  );
}
