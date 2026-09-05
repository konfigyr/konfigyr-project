'use client';

import * as React from 'react';

import { cn } from '@konfigyr/components/utils';

export function Table({ className, variant = 'default', ...props }: { variant?: 'default' | 'card' } & React.ComponentProps<'table'>) {
  return (
    <div
      data-slot="table-container"
      data-variant={variant}
      className={cn(
        'relative w-full overflow-x-auto',
        variant === 'card' && 'rounded-md border flex flex-col bg-card text-card-foreground',
      )}
    >
      <table
        data-slot="table"
        className={cn('w-full caption-bottom text-sm', className)}
        {...props}
      />
    </div>
  );
}

export function TableHeader({ className, ...props }: React.ComponentProps<'thead'>) {
  return (
    <thead
      data-slot="table-header"
      className={cn('[&_tr]:border-b-transparent [&_tr]:bg-muted [&_tr]:hover:bg-muted', className)}
      {...props}
    />
  );
}

export function TableBody({ className, ...props }: React.ComponentProps<'tbody'>) {
  return (
    <tbody
      data-slot="table-body"
      className={cn('[&_tr:last-child]:border-0', className)}
      {...props}
    />
  );
}

export function TableFooter({ className, ...props }: React.ComponentProps<'tfoot'>) {
  return (
    <tfoot
      data-slot="table-footer"
      className={cn('bg-muted/50 border-t font-medium [&>tr]:last:border-b-0', className)}
      {...props}
    />
  );
}

export function TableRow({ className, ...props }: React.ComponentProps<'tr'>) {
  return (
    <tr
      data-slot="table-row"
      className={cn('hover:bg-muted data-[state=selected]:bg-muted border-b transition-colors', className)}
      {...props}
    />
  );
}

export function TableHead({ className, ...props }: React.ComponentProps<'th'>) {
  return (
    <th
      data-slot="table-head"
      className={cn(
        'px-5 py-3 text-left text-xs align-middle font-heading font-medium uppercase tracking-widest whitespace-nowrap text-muted-foreground has-[[role=checkbox]]:pr-0',
        className,
      )}
      {...props}
    />
  );
}

export function TableCell({ className, ...props }: React.ComponentProps<'td'>) {
  return (
    <td
      data-slot="table-cell"
      className={cn(
        'px-5 py-3 align-middle whitespace-nowrap has-[[role=checkbox]]:pr-0',
        className,
      )}
      {...props}
    />
  );
}

export function TableCaption({ className, ...props }: React.ComponentProps<'caption'>) {
  return (
    <caption
      data-slot="table-caption"
      className={cn('text-muted-foreground mt-4 text-sm', className)}
      {...props}
    />
  );
}
