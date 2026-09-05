'use client';

import * as React from 'react';
import { cva } from 'class-variance-authority';
import { cn } from '@konfigyr/components/utils';

import type { VariantProps } from 'class-variance-authority';

export const cardVariants = cva(
  'group/card flex flex-col overflow-hidden rounded-md border bg-card text-card-foreground has-data-[slot=card-footer]:pb-0 has-[>img:first-child]:pt-0 *:[img:first-child]:rounded-t-md *:[img:last-child]:rounded-b-md',
  {
    variants: {
      size: {
        xs: 'gap-2 py-2 text-xs',
        sm: 'gap-2.5 py-3 text-sm',
        default: 'gap-3.5 py-4 text-sm',
        lg: 'gap-4 py-5',
      },
    },
    defaultVariants: {
      size: 'default',
    },
  },
);

export function Card({ className, size = 'default', ...props }: React.ComponentProps<'div'> & VariantProps<typeof cardVariants>) {
  return (
    <div
      data-slot="card"
      data-size={size}
      className={cn(cardVariants({ size }), className)}
      {...props}
    />
  );
}

export function CardHeader({ className, title, description, children, ...props }: {
  title?: string | React.ReactNode,
  description?: string | React.ReactNode,
} & Omit<React.ComponentProps<'div'>, 'title'>) {
  return (
    <div
      data-slot="card-header"
      className={cn(
        'group/card-header @container/card-header grid auto-rows-min items-start gap-1 rounded-t-xl',
        'px-5 group-data-[size=xs]/card:px-3  group-data-[size=sm]/card:px-4 group-data-[size=lg]/card:px-6',
        'has-data-[slot=card-action]:grid-cols-[1fr_auto] has-data-[slot=card-description]:grid-rows-[auto_auto]',
        'group-data-[size=sm]/card:[.border-b]:pb-3 [.border-b]:pb-4 group-data-[size=lg]/card:[.border-b]:pb-5',
        className,
      )}
      {...props}
    >
      {title && (
        <CardTitle>{title}</CardTitle>
      )}

      {description && (
        <CardDescription>{description}</CardDescription>
      )}

      {children}
    </div>
  );
}

export function CardIcon({ className, ...props }: React.ComponentProps<'i'>) {
  return (
    <i
      data-slot="card-icon"
      className={cn('bg-accent text-accent-foreground rounded p-2', className)}
      {...props}
    />
  );
}

export function CardTitle({ className, ...props }: React.ComponentProps<'div'>) {
  return (
    <div
      data-slot="card-title"
      className={cn(
        'font-heading text-lg leading-snug font-semibold',
        'group-data-[size=xs]/card:text-sm group-data-[size=lg]/card:text-xl',
        className,
      )}
      {...props}
    />
  );
}

export function CardDescription({ className, ...props }: React.ComponentProps<'div'>) {
  return (
    <div
      data-slot="card-description"
      className={cn('text-muted-foreground group-data-[size=xs]/card:text-xs text-sm', className)}
      {...props}
    />
  );
}

export function CardAction({ className, ...props }: React.ComponentProps<'div'>) {
  return (
    <div
      data-slot="card-action"
      className={cn('col-start-2 row-span-2 row-start-1 self-start justify-self-end', className)}
      {...props}
    />
  );
}

export function CardContent({ className, ...props }: React.ComponentProps<'div'>) {
  return (
    <div
      data-slot="card-content"
      className={cn(
        'px-5 group-data-[size=xs]/card:px-3 group-data-[size=sm]/card:px-4 group-data-[size=lg]/card:px-6',
        className,
      )}
      {...props}
    />
  );
}

export function CardFooter({ className, ...props }: React.ComponentProps<'div'>) {
  return (
    <div
      data-slot="card-footer"
      className={cn(
        'flex items-center rounded-b-xl border-t px-5 py-4',
        'group-data-[size=xs]/card:px-3 group-data-[size=xs]/card:py-2.5',
        'group-data-[size=sm]/card:px-4 group-data-[size=sm]/card:py-3',
        'group-data-[size=lg]/card:px-6 group-data-[size=sm]/card:py-4',
        className,
      )}
      {...props}
    />
  );
}
