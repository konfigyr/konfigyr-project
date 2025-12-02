'use client';

import { useEffect } from 'react';
import { NamespaceContext, useLastUsedNamespace } from '@konfigyr/hooks';

import type { ReactNode } from 'react';
import type { Namespace } from '@konfigyr/hooks/types';

export function NamespaceProvider({ namespace, children }: { namespace: Namespace, children: ReactNode }) {
  const [_, setLastUsedNamespace] = useLastUsedNamespace();

  useEffect(() => {
    setLastUsedNamespace(namespace.slug);
  }, [namespace.slug, setLastUsedNamespace]);

  return (
    <NamespaceContext.Provider value={namespace}>
      {children}
    </NamespaceContext.Provider>
  );
}
