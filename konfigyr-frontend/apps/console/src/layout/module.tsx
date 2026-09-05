'use client';

import { createContext, useContext, useMemo } from 'react';
import { useMatches } from '@tanstack/react-router';
import { createLogger } from '@konfigyr/logger';

import type { ReactNode } from 'react';
import type { ModuleType } from '@konfigyr/types';
import type { FileRouteTypes } from '@konfigyr/routeTree.gen';

const logger = createLogger('layout/module');

const MODULE_ROUTE_IDS: Record<ModuleType, FileRouteTypes['id']> = {
  overview: '/_authenticated/namespace/$namespace/_overview',
  services: '/_authenticated/namespace/$namespace/services',
  artifactory: '/_authenticated/namespace/$namespace/artifactory',
  kms: '/_authenticated/namespace/$namespace/kms',
};

const MODULE_TYPES = Object.keys(MODULE_ROUTE_IDS) as Array<ModuleType>;

function isModuleRoute(routeId: string, moduleRouteId: string) {
  return routeId === moduleRouteId || routeId.startsWith(`${moduleRouteId}/`);
}

const ModuleContext = createContext<ModuleType | undefined>(undefined);

export function ModuleProvider({ children }: { children: ReactNode }) {
  const matches = useMatches();

  const module = useMemo(() => {
    const match = MODULE_TYPES.find(
      type => matches.some(
        it => isModuleRoute(it.routeId, MODULE_ROUTE_IDS[type]),
      ),
    );

    if (match === undefined) {
      const routes = matches.map(it => it.routeId).join(', ');
      logger.warn(`Failed to determine the module type from the current route chain: ${routes}`);
    }

    return match;
  }, [matches]);

  return (
    <ModuleContext.Provider value={module}>
      {children}
    </ModuleContext.Provider>
  );
}

export function useModule() {
  return useContext(ModuleContext);
}
