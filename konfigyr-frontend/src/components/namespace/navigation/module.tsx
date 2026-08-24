import { useModule } from '@konfigyr/layout';
import { NamespaceArtifactoryNavigationMenu } from '@konfigyr/components/namespace/navigation/artifactory';
import { NamespaceNavigationMenu } from '@konfigyr/components/namespace/navigation/general';
import { NamespaceKmsNavigationMenu } from '@konfigyr/components/namespace/navigation/kms';
import { NamespaceServicesNavigationMenu } from '@konfigyr/components/namespace/navigation/services';

import type { ComponentType } from 'react';
import type { ModuleType } from '@konfigyr/types';
import type { Namespace } from '@konfigyr/hooks/types';

const MODULE_NAVIGATION: Record<ModuleType, ComponentType<{ namespace: Namespace }>> = {
  overview: NamespaceNavigationMenu,
  services: NamespaceServicesNavigationMenu,
  artifactory: NamespaceArtifactoryNavigationMenu,
  kms: NamespaceKmsNavigationMenu,
};

export function ModuleNavigation({ namespace }: { namespace: Namespace }) {
  const module = useModule();

  if (!module) {
    return null;
  }

  const Navigation = MODULE_NAVIGATION[module];

  return <Navigation namespace={namespace} />;
}
