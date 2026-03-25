'use client';

import { useCallback, useState } from 'react';
import { FormattedMessage } from 'react-intl';
import {
  CurlyBracesIcon,
  HistoryIcon,
  PencilIcon,
  TrashIcon,
  UndoIcon,
} from 'lucide-react';
import { PropertyDeprecation } from '@konfigyr/components/artifactory/property-deprecation';
import { PropertyDescription } from '@konfigyr/components/artifactory/property-description';
import { PropertyName } from '@konfigyr/components/artifactory/property-name';
import {
  ActionsLabel,
  DeleteLabel,
  HistoryLabel,
  UndoLabel,
} from '@konfigyr/components/messages';
import { InlineInputField } from '@konfigyr/components/vault/input';
import { Button } from '@konfigyr/components/ui/button';
import { ClipboardIconButton } from '@konfigyr/components/clipboard';
import {
  InlineEdit,
  InlineEditPlaceholder,
} from '@konfigyr//components/ui/inline-edit';
import { EmptyState } from '@konfigyr/components/ui/empty';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@konfigyr/components/ui/table';
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@konfigyr/components/ui/tooltip';
import { cn } from '@konfigyr/components/utils';
import { StateBadge } from './state-label';
import {
  PropertyNameLabel,
  PropertyValueLabel,
  RestorePropertyLabel,
} from './messages';

import type { ReactNode } from 'react';
import type {
  ConfigurationProperty,
  ConfigurationPropertyState,
  ConfigurationPropertyValue,
} from '@konfigyr/hooks/types';

interface PropertyActionProps {
  onDelete?: (property: ConfigurationProperty<any>) => void | Promise<void>
  onRestore?: (property: ConfigurationProperty<any>) => void | Promise<void>
  onHistory?: (property: ConfigurationProperty<any>) => void | Promise<void>
}

const statusBorderColors: Record<ConfigurationPropertyState, string> = {
  unchanged: 'border-l-transparent',
  modified: 'border-l-amber-500',
  deleted: 'border-l-destructive',
  added: 'border-l-emerald-500',
};

function PropertyNameCell<T>({ property }: { property: ConfigurationProperty<T> }) {
  const isDeleted = property.state === 'deleted';

  return (
    <div className="flex flex-col gap-1 min-w-0">
      <div className="flex items-center gap-2">
        <PropertyName
          value={property.name}
          className={cn(isDeleted && 'line-through text-muted-foreground')}
        />
        <PropertyDeprecation deprecation={property.deprecation} />
        {property.state === 'added' && (<StateBadge variant="added" />)}
        {property.state === 'modified' && (<StateBadge variant="modified" />)}
        {isDeleted && (<StateBadge variant="deleted" />)}
      </div>

      <PropertyDescription
        value={property.description}
        className={cn(isDeleted && 'line-clamp-2 line-through opacity-60')}
      />
    </div>
  );
}

function ValueCell<T>({ property, onSave }: {
  property: ConfigurationProperty<T>,
  onSave: (property: ConfigurationProperty<T>, value: ConfigurationPropertyValue<T>) => void,
}) {
  return (
    <InlineEdit value={property.value} onChange={value => onSave(property, value!)}>
      <InlineEditPlaceholder
        disabled={property.state === 'deleted'}
        className={cn(
          'font-mono text-sm font-medium text-muted-foreground rounded-md px-1.5 py-0.5 -mx-1.5 -my-0.5 transition-colors',
          property.state === 'deleted' ? 'cursor-default line-through opacity-60' : 'hover:bg-muted/80',
        )}
      >
        <div className="flex items-center gap-2">
          {property.value?.encoded && property.value.encoded.length > 40 ? (
            <Tooltip delayDuration={200}>
              <TooltipTrigger asChild>
                <span className="truncate max-w-70 block">{property.value.encoded}</span>
              </TooltipTrigger>
              <TooltipContent side="top" className="font-mono text-xs max-w-sm break-all">
                {property.value.encoded}
              </TooltipContent>
            </Tooltip>
          ) : property.value?.encoded}

          {property.state !== 'deleted' && (
            <span className="opacity-0 group-hover/inline-edit-placeholder:opacity-50 transition-opacity">
              <PencilIcon className="h-3 w-3" />
            </span>
          )}
        </div>
      </InlineEditPlaceholder>

      <InlineInputField property={property} />
    </InlineEdit>
  );
}

function ActionButton({ tooltip, destructive = false, className, children, onClick }: {
  tooltip: string | ReactNode,
  destructive?: boolean,
  className?: string,
  children?: ReactNode,
  onClick: () => void | Promise<void>,
}) {
  const [isPending, setIsPending] = useState(false);

  const onClickAction = useCallback(async () => {
    setIsPending(true);

    try {
      await onClick();
    } finally {
      setIsPending(false);
    }
  }, [onClick]);

  return (
    <Tooltip delayDuration={300}>
      <TooltipTrigger asChild>
        <Button
          size="sm"
          variant="ghost"
          loading={isPending}
          className={cn(
            'text-muted-foreground',
            destructive ? 'hover:text-destructive' : 'hover:text-foreground',
            className,
          )}
          onClick={onClickAction}
        >
          {children}
        </Button>
      </TooltipTrigger>
      <TooltipContent side="top" className="text-xs">
        {tooltip}
      </TooltipContent>
    </Tooltip>
  );
}

function ActionsCell<T>({
  property,
  onDelete,
  onRestore,
  onHistory,
}: { property: ConfigurationProperty<T> } & PropertyActionProps) {
  if (property.state === 'deleted') {
    return (
      <div className="flex items-center justify-end gap-0.5">
        {onRestore && (
          <ActionButton tooltip={<RestorePropertyLabel />} onClick={() => onRestore(property)}>
            <UndoIcon /> <UndoLabel />
          </ActionButton>
        )}
      </div>
    );
  }

  return (
    <div className="flex items-center justify-end gap-0.5 opacity-0 group-hover/row:opacity-100 transition-opacity duration-150">
      {property.value && (
        <ClipboardIconButton
          size="sm"
          variant="ghost"
          text={property.value.encoded}
          delayDuration={300}
          className="text-muted-foreground hover:text-foreground"
        />
      )}

      {onHistory && (
        <ActionButton tooltip={<HistoryLabel />} onClick={() => onHistory(property)}>
          <HistoryIcon /><span className="sr-only"><HistoryLabel /></span>
        </ActionButton>
      )}

      {onDelete && (
        <ActionButton destructive tooltip={<DeleteLabel />} onClick={() => onDelete(property)}>
          <TrashIcon /><span className="sr-only"><DeleteLabel /></span>
        </ActionButton>
      )}
    </div>
  );
}

export function PropertiesTable({
  properties,
  onHistory,
  onDelete,
  onRestore,
  onUpdate,
}: {
  properties: Array<ConfigurationProperty<any>>,
  onUpdate: (property: ConfigurationProperty<any>, value?: ConfigurationPropertyValue<any>) => void | Promise<void>,
} & PropertyActionProps) {
  return (
    <div className="rounded-lg border bg-card overflow-hidden">
      <Table>
        <TableHeader>
          <TableRow className="hover:bg-transparent bg-muted/30">
            <TableHead className="w-[50%] min-w-80 pl-5">
              <PropertyNameLabel />
            </TableHead>
            <TableHead className="w-[30%] min-w-50">
              <PropertyValueLabel />
            </TableHead>
            <TableHead className="w-[20%] min-w-40 text-right">
              <span className="sr-only">
                <ActionsLabel />
              </span>
            </TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {properties.length === 0 && (
            <TableRow>
              <TableCell colSpan={3} className="h-32 text-center text-muted-foreground">
                <EmptyState
                  icon={
                    <CurlyBracesIcon />
                  }
                  title={
                    <FormattedMessage
                      defaultMessage="No configuration properties found."
                      description="Empty state title used when no configuration propertis are found."
                    />
                  }
                  description={
                    <FormattedMessage
                      defaultMessage="This profile does not contain any configuration properties. Why don't you add one?"
                      description="Empty state description used when no configuration propertis are found. Should state that either no properties exist for a profile or nothing matches the current filter state."
                    />
                  }
                />
              </TableCell>
            </TableRow>
          )}
          {properties.map((property) => {
            return (
              <TableRow
                key={property.name}
                className={cn(
                  'group/row transition-colors border-l-[3px]!',
                  statusBorderColors[property.state],
                  property.deprecation && property.state === 'unchanged' && 'opacity-70',
                  property.state === 'deleted' && 'bg-destructive/3 opacity-80',
                )}
              >
                <TableCell className="py-3 pl-4">
                  <PropertyNameCell property={property} />
                </TableCell>
                <TableCell className="py-3">
                  <ValueCell
                    property={property}
                    onSave={onUpdate}
                  />
                </TableCell>
                <TableCell className="py-3 text-right pr-3">
                  <ActionsCell
                    property={property}
                    onDelete={onDelete}
                    onHistory={onHistory}
                    onRestore={onRestore}
                  />
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </div>
  );
}
