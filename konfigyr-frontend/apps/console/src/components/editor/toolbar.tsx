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

function ToolbarButton({ className, children, ...props }: ComponentProps<'button'>) {
  return (
    <button
      className={cn(
        'inline-flex h-7 gap-1 px-2 shrink-0 items-center justify-center cursor-pointer',
        'text-sm font-bold whitespace-nowrap rounded-md border-2 border-transparent transition-all outline-none select-none',
        '[&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg]:size-4',
        'disabled:pointer-events-none disabled:opacity-50',
        'hover:bg-accent hover:text-accent-foreground hover:border-accent-foreground/20',
        'active:bg-primary/20 active:text-accent-foreground active:border-accent-foreground/20',
        'focus-visible:bg-accent focus-visible:text-accent-foreground focus-visible:border-accent-foreground/40',
        className,
      )}
      {...props}
    >
      {children}
    </button>
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
      <ToolbarButton
        className="group/editor-btn"
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
      </ToolbarButton>

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
                  <ToolbarButton
                    className=""
                    onMouseDown={(event) => onMouseDown(event, action)}
                  >
                    {action.icon}
                    <span className="sr-only">{action.label}</span>
                  </ToolbarButton>
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
