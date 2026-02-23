import {
  NavigationMenu,
  NavigationMenuItem,
  NavigationMenuLink,
  NavigationMenuList,
  navigationMenuTriggerStyle,
} from '@konfigyr/components/ui/navigation-menu';
import { Link  } from '@tanstack/react-router';

import type { Namespace, Service } from '@konfigyr/hooks/types';

export function ServiceNavigationMenu({ namespace, service }: { namespace: Namespace, service: Service }) {
  return (
    <NavigationMenu>
      <NavigationMenuList>
        <NavigationMenuItem>
          <NavigationMenuLink asChild>
            <Link
              to="/namespace/$namespace/services/$service"
              params={{ namespace: namespace.slug, service: service.slug }}
              activeProps={{ 'data-active': true }}
              activeOptions={{ exact: true }}
              className={navigationMenuTriggerStyle()}
            >
              Overview
            </Link>
          </NavigationMenuLink>
        </NavigationMenuItem>

        <NavigationMenuItem>
          <NavigationMenuLink asChild>
            <Link
              to="/namespace/$namespace/services/$service/requests"
              params={{ namespace: namespace.slug, service: service.slug }}
              activeProps={{ 'data-active': true }}
              activeOptions={{ exact: true }}
              className={navigationMenuTriggerStyle()}
            >
              Change requests
            </Link>
          </NavigationMenuLink>
        </NavigationMenuItem>

        <NavigationMenuItem>
          <NavigationMenuLink asChild>
            <Link
              to="/namespace/$namespace/services/$service/settings"
              params={{ namespace: namespace.slug, service: service.slug }}
              activeProps={{ 'data-active': true }}
              activeOptions={{ exact: true }}
              className={navigationMenuTriggerStyle()}
            >
              Settings
            </Link>
          </NavigationMenuLink>
        </NavigationMenuItem>
      </NavigationMenuList>
    </NavigationMenu>
  );
}
