'use client';

import { cva } from 'class-variance-authority';
import { Avatar as AvatarPrimitive } from '@base-ui/react/avatar';
import { cn } from '@konfigyr/components/utils';

import type { ComponentProps } from 'react';
import type { VariantProps } from 'class-variance-authority';

export const avatarVariants = cva(
  'relative flex shrink-0 overflow-hidden border rounded-full',
  {
    variants: {
      size: {
        xs: 'size-5',
        sm: 'size-6',
        default: 'size-8',
        lg: 'size-10',
        xl: 'size-14',
        '2xl': 'size-20',
      },
    },
    defaultVariants: {
      size: 'default',
    },
  },
);

export function Avatar({ size, className, ...props }: ComponentProps<typeof AvatarPrimitive.Root> & VariantProps<typeof avatarVariants>) {
  return (
    <AvatarPrimitive.Root
      data-slot="avatar"
      data-size={size}
      className={cn(avatarVariants({ size, className }))}
      {...props}
    />
  );
}

export function AvatarImage({ className, ...props }: ComponentProps<typeof AvatarPrimitive.Image>) {
  return (
    <AvatarPrimitive.Image
      data-slot="avatar-image"
      className={cn('"aspect-square size-full rounded-full object-cover', className)}
      {...props}
    />
  );
}

export function AvatarFallback({ className, ...props }: ComponentProps<typeof AvatarPrimitive.Fallback>) {
  return (
    <AvatarPrimitive.Fallback
      data-slot="avatar-fallback"
      className={cn(
        'flex size-full items-center justify-center rounded-full bg-muted text-sm text-muted-foreground',
        'group-data-[size=xs]/avatar:text-[10px] group-data-[size=sm]/avatar:text-xs',
        'group-data-[size=lg]/avatar:text-base group-data-[size=xl]/avatar:text-xl group-data-[size=2xl]/avatar:text-3xl',
        className,
      )}
      {...props}
    />
  );
}

export function AvatarBadge({ className, ...props }: ComponentProps<'span'>) {
  return (
    <span
      data-slot="avatar-badge"
      className={cn(
        'absolute right-0 bottom-0 z-10 inline-flex items-center justify-center rounded-full bg-primary text-primary-foreground bg-blend-color ring-2 ring-background select-none',
        'group-data-[size=xs]/avatar:size-1.5 group-data-[size=xs]/avatar:[&>svg]:hidden',
        'group-data-[size=sm]/avatar:size-2 group-data-[size=sm]/avatar:[&>svg]:hidden',
        'group-data-[size=default]/avatar:size-2.5 group-data-[size=default]/avatar:[&>svg]:size-2',
        'group-data-[size=lg]/avatar:size-3 group-data-[size=lg]/avatar:[&>svg]:size-2',
        'group-data-[size=xl]/avatar:size-3.5 group-data-[size=xl]/avatar:[&>svg]:size-2',
        'group-data-[size=2xl]/avatar:size-4 group-data-[size=2xl]/avatar:[&>svg]:size-2.5',
        className,
      )}
      {...props}
    />
  );
}

export function AvatarGroup({ className, ...props }: ComponentProps<'div'>) {
  return (
    <div
      data-slot="avatar-group"
      className={cn('group/avatar-group flex -space-x-2 *:data-[slot=avatar]:ring-2 *:data-[slot=avatar]:ring-background', className)}
      {...props}
    />
  );
}

export function AvatarGroupCount({ className, ...props }: ComponentProps<'div'>) {
  return (
    <div
      data-slot="avatar-group-count"
      className={cn(
        'relative flex size-8 shrink-0 items-center justify-center rounded-full bg-muted text-sm text-muted-foreground ring-2 ring-background [&>svg]:size-4',
        'group-has-data-[size=xs]/avatar-group:size-5 group-has-data-[size=xs]/avatar-group:[&>svg]:size-2.5',
        'group-has-data-[size=sm]/avatar-group:size-6 group-has-data-[size=sm]/avatar-group:[&>svg]:size-3',
        'group-has-data-[size=lg]/avatar-group:size-10 group-has-data-[size=lg]/avatar-group:[&>svg]:size-5',
        'group-has-data-[size=xl]/avatar-group:size-14 group-has-data-[size=xl]/avatar-group:[&>svg]:size-6',
        'group-has-data-[size=2xl]/avatar-group:size-20 group-has-data-[size=2xl]/avatar-group:[&>svg]:size-8',
        className,
      )}
      {...props}
    />
  );
}
