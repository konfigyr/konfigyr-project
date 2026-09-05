import { SearchIcon } from 'lucide-react';
import { EmptyState } from '@konfigyr/components/ui/empty';
import { Card, CardContent } from '@konfigyr/components/ui/card';
import { ItemGroup } from '@konfigyr/components/ui/item';
import { PageResponsePagination } from '@konfigyr/components/ui/pagination';
import { NoMatchingPropertiesTitle } from './messages';
import { PropertyItem } from './property-item';

import type { PageResponse } from '@konfigyr/hooks/hateoas/types';
import type { PropertyDefinition } from '@konfigyr/hooks/artifactory/types';
import type { PropertyVariant } from './property-item';

export type PropertyTableProps = {
  properties: PageResponse<PropertyDefinition>,
  variant?: PropertyVariant,
  page?: number;
  size?: number;
};

export function PropertyTable({ properties, variant = 'default', page, size }: PropertyTableProps) {
  return (
    <>
      <Card className="border">
        <CardContent>
          {properties.data.length === 0 && (
            <EmptyState
              icon={<SearchIcon size="2rem"/>}
              title={<NoMatchingPropertiesTitle/>}
            />
          )}

          <ItemGroup>
            {properties.data.map(property => (
              <PropertyItem
                key={property.id}
                property={property}
                variant={variant}
              />
            ))}
          </ItemGroup>
        </CardContent>
      </Card>

      <PageResponsePagination page={page} size={size} response={properties} className="mt-4" />
    </>
  );
}
