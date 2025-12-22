'use client';

import * as React from 'react';
import { cn } from '@konfigyr/components/utils';

export function Card({ className, ...props }: React.ComponentProps<'div'>) {
  return (
    <div
      data-slot="card"
      className={cn(
        'bg-card text-card-foreground flex flex-col gap-4 rounded-xl shadow-xs py-4',
        className,
      )}
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
        '@container/card-header grid auto-rows-min grid-rows-[auto_auto] items-start gap-1.5 px-6 has-[data-slot=card-action]:grid-cols-[1fr_auto] [.border-b]:pb-4',
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
      className={cn('bg-muted text-foreground rounded p-2', className)}
      {...props}
    />
  );
}

export function CardTitle({ className, ...props }: React.ComponentProps<'div'>) {
  return (
    <div
      data-slot="card-title"
      className={cn('leading-none text-lg font-medium', className)}
      {...props}
    />
  );
}

export function CardDescription({ className, ...props }: React.ComponentProps<'div'>) {
  return (
    <div
      data-slot="card-description"
      className={cn('text-muted-foreground text-sm', className)}
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
      className={cn('px-6', className)}
      {...props}
    />
  );
}

export function CardFooter({ className, ...props }: React.ComponentProps<'div'>) {
  return (
    <div
      data-slot="card-footer"
      className={cn('flex items-center px-6 [.border-t]:pt-4', className)}
      {...props}
    />
  );
}
