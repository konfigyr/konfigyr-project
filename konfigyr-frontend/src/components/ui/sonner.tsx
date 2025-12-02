'use client';

import { Toaster as Sonner } from 'sonner';

import type { CSSProperties } from 'react';
import type { ToasterProps } from 'sonner';

const STYLES = {
  '--normal-bg': 'hsl(var(--popover))',
  '--normal-text': 'hsl(var(--popover-foreground))',
  '--normal-border': 'var(--border)',
} as CSSProperties;

export function Toaster({ ...props }: ToasterProps) {
  return (
    <Sonner className='toaster group' style={STYLES} {...props} />
  );
}
