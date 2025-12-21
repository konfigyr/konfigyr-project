import { cva } from 'class-variance-authority';
import { cn } from '@konfigyr/components/utils';

import type { ComponentProps, ReactNode } from 'react';
import type { VariantProps } from 'class-variance-authority';

export const emptyVariants = cva(
  'flex min-w-0 flex-1 flex-col items-center justify-center rounded-lg border-dashed text-center text-balance',
  {
    variants: {
      size: {
        default: 'gap-6 p-6 md:p-12',
        sm: 'gap-2 p-2 md:p-4',
        lg: 'gap-6 p-8 md:p-16',
      },
    },
    defaultVariants: {
      size: 'default',
    },
  },
);

export function Empty({ size, className, ...props }: ComponentProps<'div'> & VariantProps<typeof emptyVariants>) {
  return (
    <div
      data-slot="empty"
      className={emptyVariants({ size, className })}
      {...props}
    />
  );
}

export function EmptyHeader({ className, ...props }: ComponentProps<'div'>) {
  return (
    <div
      data-slot="empty-header"
      className={cn('flex max-w-sm flex-col items-center gap-2 text-center', className)}
      {...props}
    />
  );
}

const emptyMediaVariants = cva(
  'flex shrink-0 items-center justify-center mb-2 [&_svg]:pointer-events-none [&_svg]:shrink-0',
  {
    variants: {
      variant: {
        default: 'bg-transparent',
        icon: 'bg-muted text-foreground flex shrink-0 items-center justify-center rounded-lg',
      },
      size: {
        default: 'size-10 [&_svg:not([class*=\'size-\'])]:size-6',
        sm: 'size-6 [&_svg:not([class*=\'size-\'])]:size-4',
        lg: 'size-14 [&_svg:not([class*=\'size-\'])]:size-8',
      },
    },
    defaultVariants: {
      variant: 'default',
    },
  },
);

export function EmptyMedia({ className, size = 'default', variant = 'default', ...props }: ComponentProps<'div'> & VariantProps<typeof emptyMediaVariants>) {
  return (
    <div
      data-slot="empty-icon"
      data-variant={variant}
      className={cn(emptyMediaVariants({ size, variant, className }))}
      {...props}
    />
  );
}

export const emptyTitleVariants = cva(
  'tracking-tight',
  {
    variants: {
      size: {
        default: 'text-lg font-medium',
        sm: 'font-semibold',
        lg: 'text-xl font-medium',
      },
    },
    defaultVariants: {
      size: 'default',
    },
  },
);

export function EmptyTitle({ size, className, ...props }: ComponentProps<'div'> & VariantProps<typeof emptyTitleVariants>) {
  return (
    <div
      data-slot="empty-title"
      className={emptyTitleVariants({ size, className })}
      {...props}
    />
  );
}

export const emptyDescriptionVariants = cva(
  'text-muted-foreground [&>a:hover]:text-primary [&>a]:underline [&>a]:underline-offset-4',
  {
    variants: {
      size: {
        default: 'text-sm/relaxed',
        sm: 'text-xs/relaxed',
        lg: 'relaxed',
      },
    },
    defaultVariants: {
      size: 'default',
    },
  },
);

export function EmptyDescription({ size, className, ...props }: ComponentProps<'p'> & VariantProps<typeof emptyDescriptionVariants>) {
  return (
    <div
      data-slot="empty-description"
      className={emptyDescriptionVariants({ size, className})}
      {...props}
    />
  );
}

export function EmptyContent({ className, ...props }: ComponentProps<'div'>) {
  return (
    <div
      data-slot="empty-content"
      className={cn('flex w-full max-w-sm min-w-0 flex-col items-center gap-4 text-sm text-balance', className)}
      {...props}
    />
  );
}

export function EmptyState({ title, description, icon, children, ...props }: ComponentProps<typeof Empty> & {
  title: string | ReactNode;
  description?: string | ReactNode;
  icon?: ReactNode;
}) {
  return (
    <Empty {...props}>
      <EmptyHeader>
        {icon && (
          <EmptyMedia size={props.size} variant="icon">
            {icon}
          </EmptyMedia>
        )}

        <EmptyTitle size={props.size}>
          {title}
        </EmptyTitle>

        {description && (
          <EmptyDescription size={props.size}>
            {description}
          </EmptyDescription>
        )}
      </EmptyHeader>

      {children && (
        <EmptyContent>
          {children}
        </EmptyContent>
      )}
    </Empty>
  );
}
