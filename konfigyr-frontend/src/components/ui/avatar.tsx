'use client';

import * as React from 'react';
import * as AvatarPrimitive from '@radix-ui/react-avatar';
import { cva, type VariantProps } from 'class-variance-authority';

import { cn } from 'konfigyr/components/utils';

export const avatarVariants = cva(
  'relative flex shrink-0 overflow-hidden border rounded-full',
  {
    variants: {
      size: {
        default: 'size-8',
        sm: 'size-6',
        lg: 'size-12',
      },
    },
    defaultVariants: {
      size: 'default',
    },
  },
);


export type AvatarProps = React.ComponentProps<typeof AvatarPrimitive.Root> & VariantProps<typeof avatarVariants>;

export function Avatar({ size, className, ...props }: AvatarProps) {
  return (
    <AvatarPrimitive.Root
      data-slot="avatar"
      className={cn(avatarVariants({ size, className }))}
      {...props}
    />
  );
}

export function AvatarImage({ className, ...props }: React.ComponentProps<typeof AvatarPrimitive.Image>) {
  return (
    <AvatarPrimitive.Image
      data-slot="avatar-image"
      className={cn('aspect-square size-full', className)}
      {...props}
    />
  );
}

export function AvatarFallback({ className, ...props }: React.ComponentProps<typeof AvatarPrimitive.Fallback>) {
  return (
    <AvatarPrimitive.Fallback
      data-slot="avatar-fallback"
      className={cn('bg-muted flex size-full items-center justify-center rounded-full', className)}
      {...props}
    />
  );
}
