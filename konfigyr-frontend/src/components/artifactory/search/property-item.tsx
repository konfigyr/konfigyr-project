import { BoxIcon, BuildingIcon } from 'lucide-react';
import { Item, ItemContent, ItemFooter, ItemTitle } from '@konfigyr/components/ui/item';
import { Skeleton } from '@konfigyr/components/ui/skeleton';
import { PropertyName } from '@konfigyr/components/artifactory/property-name';
import { PropertyTypeName } from '@konfigyr/components/artifactory/property-type-name';
import { PropertyDeprecation } from '@konfigyr/components/artifactory/property-deprecation';
import { PropertyDescription } from '@konfigyr/components/artifactory/property-description';
import { PropertyDefaultValue } from '@konfigyr/components/artifactory/property-default-value';
import { PropertySchema } from '@konfigyr/components/artifactory/property-schema';
import { OwnedByLabel } from '@konfigyr/components/artifactory/search/messages';

import type { PropertyDefinition } from '@konfigyr/hooks/artifactory/types';

export type PropertyVariant = 'default' | 'version';

export function PropertySkeleton({ variant = 'default' }: { variant?: PropertyVariant }) {
  return (
    <div data-slot="property-search-skeleton" className="border border-accent rounded-xl p-4">
      <div className="flex items-center gap-2">
        <Skeleton className="h-4 w-72 mb-2"/>
        <Skeleton className="h-4 w-16 mb-2"/>
      </div>
      <Skeleton className="h-4 w-48 mb-3"/>
      <Skeleton className="h-4 w-56 mb-1"/>
      <Skeleton className="h-4 w-64"/>
      {variant === 'default' && (
        <div className="flex items-center gap-2 mt-4">
          <Skeleton className="h-4 w-56"/>
          <Skeleton className="h-4 w-42"/>
        </div>
      )}
    </div>
  );
}

export function PropertyItem({ property, variant = 'default' }: { property: PropertyDefinition, variant?: PropertyVariant }) {
  return (
    <Item variant="list">
      <ItemContent>
        <ItemTitle>
          <PropertyName value={property.name}/>
          <PropertyTypeName value={property.typeName}/>
          <PropertyDeprecation deprecation={property.deprecation}/>
        </ItemTitle>
        <PropertyDescription value={property.description}/>
        <div className="text-xs mt-2 space-y-1">
          <PropertyDefaultValue variant="labeled" value={property.defaultValue}/>
          <p>
            <span className="text-muted-foreground mr-1">JSON Schema type:</span>
            <PropertySchema value={property.schema}/>
          </p>
        </div>
        {variant === 'default' && (
          <ItemFooter className="mt-2">
            <ol className="flex gap-2 text-muted-foreground font-sm [&_svg]:size-4">
              <li className="flex items-center gap-1">
                <BoxIcon />
                <span>{property.key}</span>
              </li>
              <li>·</li>
              <li className="flex items-center align-center gap-1">
                <BuildingIcon /> <OwnedByLabel owner={property.owner.slug} />
              </li>
            </ol>
          </ItemFooter>
        )}
      </ItemContent>
    </Item>
  );
}
