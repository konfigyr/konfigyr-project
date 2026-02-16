import { useCallback, useState } from 'react';
import { ListPlusIcon, PlusIcon } from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import { Button } from '@konfigyr/components/ui/button';
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@konfigyr/components/ui/dialog';
import { Input } from '@konfigyr/components/ui/input';
import { Label } from '@konfigyr/components/ui/label';
import { CancelLabel } from '@konfigyr/components/messages';
import {
  AddPropertyLabel,
  PropertyNameLabel,
  PropertyValueLabel,
} from './messages';

import type { ChangesetState, ConfigurationProperty } from '@konfigyr/hooks/types';

export function PropertyDialog({ changeset, onAdd }: {
  changeset: ChangesetState,
  onAdd: (property: ConfigurationProperty) => void | Promise<unknown>,
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
        value,
        state: 'added',
        type: 'java.lang.String',
        schema: { type: 'string' },
      });

      onOpenChange(false);
      onNameChange('');
      onValueChange('');
    }
  }, [canAdd, name, value]);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogTrigger asChild>
        <Button variant="outline">
          <PlusIcon />
          <AddPropertyLabel />
        </Button>
      </DialogTrigger>
      <DialogContent className="sm:max-w-140 gap-0 p-0 overflow-hidden">
        <DialogHeader className="px-6 pt-6 pb-4">
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

        <div className="px-6 space-y-4 pb-2">
          <div className="space-y-2">
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
          </div>

          <div className="space-y-2">
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
          </div>
        </div>

        <DialogFooter className="px-6 py-4 border-t">
          <DialogClose asChild>
            <Button variant="outline" size="sm">
              <CancelLabel />
            </Button>
          </DialogClose>
          <Button
            size="sm"
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
