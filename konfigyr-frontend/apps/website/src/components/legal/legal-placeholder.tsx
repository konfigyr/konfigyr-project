import type { ReactNode } from 'react';

export function LegalPlaceholder({ children }: { children: ReactNode }) {
  return (
    <span className="text-warning-foreground bg-warning/20 rounded px-1">
      {children}
    </span>
  );
}
