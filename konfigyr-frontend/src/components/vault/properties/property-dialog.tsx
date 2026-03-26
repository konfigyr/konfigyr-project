import { useCallback, useState } from 'react';
import { ListPlusIcon, PlusIcon } from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import { Button } from '@konfigyr/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@konfigyr/components/ui/dialog';
import { Field, FieldGroup } from '@konfigyr/components/ui/field';
import { Input } from '@konfigyr/components/ui/input';
import { Label } from '@konfigyr/components/ui/label';
import {
  AddPropertyLabel,
  PropertyNameLabel,
  PropertyValueLabel,
} from './messages';

import type { ChangesetState, ConfigurationProperty } from '@konfigyr/hooks/types';

export function PropertyDialog<T>({ changeset, onAdd }: {
  changeset: ChangesetState,
  onAdd: (property: ConfigurationProperty<T>) => void | Promise<unknown>,
}) {
  const [open, onOpenChange] = useState(false);
  const [name, onNameChange] = useState('');
  const [value, onValueChange] = useState('');

  const canAdd = changeset.properties
    .filter(it => it.name === name)
    .length === 0;

  const handleAdd = useCallback(() => {
    if (canAdd && name && value) {
      onAdd({
        name,
        value: { encoded: value, decoded: value as T },
        state: 'added',
        typeName: 'java.lang.String',
        schema: { type: 'string' },
      });

      onOpenChange(false);
      onNameChange('');
      onValueChange('');
    }
  }, [canAdd, name, value]);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogTrigger
        render={
          <Button variant="outline">
            <PlusIcon />
            <AddPropertyLabel />
          </Button>
        }
      />
      <DialogContent className="sm:max-w-140">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2 text-lg">
            <ListPlusIcon size="1rem" />
            <FormattedMessage
              defaultMessage="Add configuration property"
              description="Label for the dialog that allows adding a new configuration property"
            />
          </DialogTitle>
          <DialogDescription>
            <FormattedMessage
              defaultMessage="Select a configuration option to add it to the changeset."
              description="Description for the dialog that allows adding a new configuration property"
            />
          </DialogDescription>
        </DialogHeader>

        <FieldGroup>
          <Field>
            <Label htmlFor="property.name">
              <PropertyNameLabel />
            </Label>
            <Input
              id="property.name"
              autoComplete="off"
              spellCheck={false}
              placeholder="e.g. spring.datasource.url"
              value={name}
              onChange={event => onNameChange(event.target.value)}
              className="font-mono"
            />
          </Field>
          <Field>
            <Label htmlFor="property.value">
              <PropertyValueLabel />
            </Label>
            <Input
              id="property.value"
              autoComplete="off"
              spellCheck={false}
              value={value}
              onChange={event => onValueChange(event.target.value)}
            />
          </Field>
        </FieldGroup>

        <DialogFooter showCloseButton={true}>
          <Button
            disabled={!canAdd}
            onClick={handleAdd}
          >
            <PlusIcon />
            <AddPropertyLabel />
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
