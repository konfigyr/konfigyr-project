import { useCallback } from 'react';
import { FormattedMessage } from 'react-intl';
import {
  BoldIcon,
  ItalicIcon,
  LinkIcon,
  ListChecksIcon,
  ListIcon,
  ListOrderedIcon,
  QuoteIcon, SquarePenIcon,
  StrikethroughIcon, ViewIcon,
} from 'lucide-react';

import { commands } from '@uiw/react-md-editor';

import { Button } from '@konfigyr/components/ui/button';
import { Kbd } from '@konfigyr/components/ui/kbd';
import { Separator } from '@konfigyr/components/ui/separator';
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@konfigyr/components/ui/tooltip';
import { cn } from '@konfigyr/components/utils';

import type { ComponentProps, ReactNode, SyntheticEvent } from 'react';
import type { ICommand } from '@uiw/react-md-editor';

type Shortcut = {
  key: string;
  alt?: boolean;
  shift?: boolean;
};

type ToolbarAction<T> = {
  id: string;
  icon: ReactNode;
  label: ReactNode;
  shortcut: Shortcut;
  command: ICommand<T>;
};

const TOOLBAR_ACTIONS: Array<(ToolbarAction<any> | 'separator')> = [
  {
    id: 'bold',
    command: commands.bold,
    shortcut: { key: 'b' },
    icon: <BoldIcon size={15} strokeWidth={1.8} />,
    label: <FormattedMessage
      defaultMessage="Bold"
      description="Editor toolbar label for the bold action"
    />,
  },
  {
    id: 'italic',
    command: commands.italic,
    shortcut: { key: 'i' },
    icon: <ItalicIcon size={15} strokeWidth={1.8} />,
    label: <FormattedMessage
      defaultMessage="Italic"
      description="Editor toolbar label for the italic action"
    /> ,
  },
  {
    id: 'strikethrough',
    command: commands.strikethrough,
    shortcut: { key: 's', shift: true },
    icon: <StrikethroughIcon size={15} strokeWidth={1.8} />,
    label: <FormattedMessage
      defaultMessage="Strikethrough"
      description="Editor toolbar label for the strikethrough action"
    />,
  },
  'separator',
  {
    id: 'ul',
    command: commands.unorderedListCommand,
    shortcut: { key: 'u', shift: true },
    icon: <ListIcon size={15} strokeWidth={1.8} />,
    label: <FormattedMessage
      defaultMessage="Unordered list"
      description="Editor toolbar label for the unordered list action"
    />,
  },
  {
    id: 'ol',
    command: commands.orderedListCommand,
    shortcut: { key: 'o', shift: true },
    icon: <ListOrderedIcon size={15} strokeWidth={1.8} />,
    label: <FormattedMessage
      defaultMessage="Ordered list"
      description="Editor toolbar label for the ordered list action"
    />,
  },
  {
    id: 'blockquote',
    command: commands.quote,
    shortcut: { key: 'b', shift: true },
    icon: <QuoteIcon size={15} strokeWidth={1.8} />,
    label: <FormattedMessage
      defaultMessage="Blockquote"
      description="Editor toolbar label for the blockquote action"
    />,
  },
  'separator',
  {
    id: 'link',
    command: commands.link,
    shortcut: { key: 'l', shift: true },
    icon: <LinkIcon size={15} strokeWidth={1.8} />,
    label: <FormattedMessage
      defaultMessage="Insert link"
      description="Editor toolbar label for the insert link action"
    />,
  },
  {
    id: 'task',
    command: commands.checkedListCommand,
    shortcut: { key: 'k', shift: true },
    icon: <ListChecksIcon size={15} strokeWidth={1.8} />,
    label: <FormattedMessage
      defaultMessage="Task link"
      description="Editor toolbar label for the task list action"
    />,
  },
];

function ActionShortcut({ shortcut }: { shortcut: Shortcut }) {
  const isMac = typeof navigator !== 'undefined' && /mac/i.test(navigator.platform);

  const key = [
    isMac ? '⌘' : 'Ctrl',
    ...(shortcut.shift ? ['Shift'] : []),
    ...(shortcut.alt ? ['Alt'] : []),
    shortcut.key.toUpperCase(),
  ].join(' + ');

  return (
    <Kbd>{key}</Kbd>
  );
}

export function ToolbarPlugin({
  editing = true,
  onCommand,
  onEditing,
  className,
  ...props
}: {
  editing?: boolean;
  onCommand: (command: ICommand) => void;
  onEditing: (editing: boolean) => void;
  className?: string;
} & ComponentProps<'div'>) {
  const onToggleEditing = useCallback(
    () => onEditing(!editing),
    [editing, onEditing],
  );

  const onMouseDown = useCallback(
    (event: SyntheticEvent, action: ToolbarAction<any>) => {
      event.preventDefault();
      event.stopPropagation();

      onCommand(action.command as ICommand);
    },
    [onCommand],
  );

  return (
    <div
      role="toolbar"
      data-slot="editor-toolbar"
      className={cn(
        'flex items-center justify-between px-2.5 py-1 border-b border-input bg-muted/40',
        className,
      )}
      {...props}
    >
      <Button
        variant="outline"
        size="sm"
        className="group/editor-btn cursor-pointer px-4"
        onClick={onToggleEditing}
      >
        {editing ? (
          <>
            <ViewIcon />
            <FormattedMessage
              defaultMessage="Preview"
              description="Label for the preview button in the editor"
            />
          </>
        ) : (
          <>
            <SquarePenIcon />
            <FormattedMessage
              defaultMessage="Edit"
              description="Label for the edit button in the editor"
            />
          </>
        )}
      </Button>

      <div className="flex items-center gap-1 flex-wrap">
        {TOOLBAR_ACTIONS.map((action, i) => {
          if (action === 'separator') {
            return (
              <Separator
                key={`sep-${i}`}
                orientation="vertical"
                className="m-1 bg-gray-300"
              />
            );
          }

          return (
            <Tooltip key={action.id}>
              <TooltipTrigger
                render={
                  <Button
                    variant="ghost"
                    size="icon-sm"
                    className="cursor-pointer hover:text-foreground hover:bg-gray-200"
                    onMouseDown={(event) => onMouseDown(event, action)}
                  >
                    {action.icon}
                    <span className="sr-only">{action.label}</span>
                  </Button>
                }
              />
              <TooltipContent side="top" className="text-xs">
                {action.label}
                <ActionShortcut shortcut={action.shortcut} />
              </TooltipContent>
            </Tooltip>
          );
        })}
      </div>
    </div>
  );
}
