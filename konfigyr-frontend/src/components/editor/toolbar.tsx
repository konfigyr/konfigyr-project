import { useCallback, useEffect } from 'react';
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

import {
  $createParagraphNode,
  $createTextNode,
  $getSelection,
  $insertNodes,
  $isRangeSelection,
  COMMAND_PRIORITY_NORMAL,
  KEY_DOWN_COMMAND,
} from 'lexical';
import { useLexicalComposerContext } from '@lexical/react/LexicalComposerContext';

import { Button } from '@konfigyr/components/ui/button';
import { Kbd } from '@konfigyr/components/ui/kbd';
import { Separator } from '@konfigyr/components/ui/separator';
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@konfigyr/components/ui/tooltip';
import { cn } from '@konfigyr/components/utils';
import { useEditorState } from './context';

import type { ComponentProps, ReactNode, SyntheticEvent } from 'react';
import type { LexicalEditor, TextNode } from 'lexical';

type Shortcut = {
  key: string;
  alt?: boolean;
  shift?: boolean;
};

type WrapAction = {
  kind: 'wrap';
  before: string;
  after: string;
};

type PrefixAction = {
  kind: 'prefix';
  prefix: string;
};

type SnippetAction = {
  kind: 'snippet';
  snippet: string;
};

type ToolbarAction = {
  id: string;
  icon: ReactNode;
  label: ReactNode;
  shortcut: Shortcut;
} & (WrapAction | PrefixAction | SnippetAction);

const TOOLBAR_ACTIONS: Array<(ToolbarAction | 'separator')> = [
  {
    id: 'bold',
    shortcut: { key: 'b' },
    icon: <BoldIcon size={15} strokeWidth={1.8} />,
    label: <FormattedMessage
      defaultMessage="Bold"
      description="Editor toolbar label for the bold action"
    />,
    kind: 'wrap',
    before: '**',
    after: '**',
  },
  {
    id: 'italic',
    shortcut: { key: 'i' },
    icon: <ItalicIcon size={15} strokeWidth={1.8} />,
    label: <FormattedMessage
      defaultMessage="Italic"
      description="Editor toolbar label for the italic action"
    /> ,
    kind: 'wrap',
    before: '_',
    after: '_',
  },
  {
    id: 'strikethrough',
    shortcut: { key: 's', shift: true },
    icon: <StrikethroughIcon size={15} strokeWidth={1.8} />,
    label: <FormattedMessage
      defaultMessage="Strikethrough"
      description="Editor toolbar label for the strikethrough action"
    />,
    kind: 'wrap',
    before: '~~',
    after: '~~',
  },
  'separator',
  {
    id: 'ul',
    shortcut: { key: 'u', shift: true },
    icon: <ListIcon size={15} strokeWidth={1.8} />,
    label: <FormattedMessage
      defaultMessage="Unordered list"
      description="Editor toolbar label for the unordered list action"
    />,
    kind: 'prefix',
    prefix: '- ',
  },
  {
    id: 'ol',
    shortcut: { key: 'o', shift: true },
    icon: <ListOrderedIcon size={15} strokeWidth={1.8} />,
    label: <FormattedMessage
      defaultMessage="Ordered list"
      description="Editor toolbar label for the ordered list action"
    />,
    kind: 'prefix',
    prefix: '1. ',
  },
  {
    id: 'blockquote',
    shortcut: { key: 'b', shift: true },
    icon: <QuoteIcon size={15} strokeWidth={1.8} />,
    label: <FormattedMessage
      defaultMessage="Blockquote"
      description="Editor toolbar label for the blockquote action"
    />,
    kind: 'prefix',
    prefix: '> ',
  },
  'separator',
  {
    id: 'link',
    shortcut: { key: 'l', shift: true },
    icon: <LinkIcon size={15} strokeWidth={1.8} />,
    label: <FormattedMessage
      defaultMessage="Insert link"
      description="Editor toolbar label for the insert link action"
    />,
    kind: 'snippet',
    snippet: '[text](url)',
  },
  {
    id: 'task',
    shortcut: { key: 'k', shift: true },
    icon: <ListChecksIcon size={15} strokeWidth={1.8} />,
    label: <FormattedMessage
      defaultMessage="Task link"
      description="Editor toolbar label for the task list action"
    />,
    kind: 'prefix',
    prefix: '- [ ] ',
  },
];

function applyWrap(editor: LexicalEditor, before: string, after: string) {
  editor.update(() => {
    const selection = $getSelection();

    if (!$isRangeSelection(selection)) {
      return;
    }

    const selectedText = selection.getTextContent();

    if (selectedText.length > 0) {
      // Replace selected text with a wrapped version
      selection.insertNodes([
        $createTextNode(`${before}${selectedText}${after}`),
      ]);
    } else {
      // No selection — insert syntax markers and place the cursor between them
      selection.insertNodes([
        $createTextNode(before),
        $createTextNode(after),
      ]);

      // Move the caret to between the two markers
      editor.update(() => {
        const newSelection = $getSelection();
        if ($isRangeSelection(newSelection)) {
          const anchor = newSelection.anchor;
          const target = anchor.getNode() as TextNode;

          // Place the cursor before the closing marker
          newSelection.setTextNodeRange(
            target,
            anchor.offset - after.length,
            target,
            anchor.offset - after.length,
          );
        }
      });
    }
  });
}

function applyPrefix(editor: LexicalEditor, prefix: string) {
  editor.update(() => {
    const selection = $getSelection();

    if (!$isRangeSelection(selection)) {
      return;
    }

    const anchorNode = selection.anchor.getNode();

    // Walk up to the top-level paragraph that contains the caret and grab the text content
    const topLevelElement = anchorNode.getTopLevelElementOrThrow();
    const currentText = topLevelElement.getTextContent();

    // If the prefix is already applied, remove it (toggle behavior)
    if (currentText.startsWith(prefix)) {
      const paragraphNode = $createParagraphNode();
      paragraphNode.append($createTextNode(currentText.slice(prefix.length)));
      topLevelElement.replace(paragraphNode);

      // Restore caret at same relative offset
      const newSelection = $getSelection();

      if ($isRangeSelection(newSelection)) {
        const offset = Math.max(0, selection.anchor.offset - prefix.length);
        newSelection.anchor.set(paragraphNode.getFirstChildOrThrow().getKey(), offset, 'text');
        newSelection.focus.set(paragraphNode.getFirstChildOrThrow().getKey(), offset, 'text');
      }
      return;
    }

    // Prepend prefix to the paragraph content
    const paragraphNode = $createParagraphNode();
    paragraphNode.append($createTextNode(`${prefix}${currentText}`));
    topLevelElement.replace(paragraphNode);

    // Restore caret at same relative offset + prefix length
    const newSelection = $getSelection();
    if ($isRangeSelection(newSelection)) {
      const offset = selection.anchor.offset + prefix.length;
      newSelection.anchor.set(paragraphNode.getFirstChildOrThrow().getKey(), offset, 'text');
      newSelection.focus.set(paragraphNode.getFirstChildOrThrow().getKey(), offset, 'text');
    }
  });
}

function applySnippet(editor: LexicalEditor, snippet: string) {
  editor.update(() => {
    const selection = $getSelection();

    if ($isRangeSelection(selection)) {
      $insertNodes([
        $createTextNode(snippet),
      ]);
    }
  });
}

function matchesShortcut(e: KeyboardEvent, shortcut: Shortcut): boolean {
  const isMac = typeof navigator !== 'undefined' && /mac/i.test(navigator.platform);
  const primaryMod = isMac ? e.metaKey : e.ctrlKey;

  if (!primaryMod) return false;
  if (e.key.toLowerCase() !== shortcut.key.toLowerCase()) return false;
  if (!!shortcut.shift !== e.shiftKey) return false;
  return !!shortcut.alt === e.altKey;
}

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

export function ToolbarPlugin({ className, ...props }: { className?: string } & ComponentProps<'div'>) {
  const [editor] = useLexicalComposerContext();
  const { editing } = useEditorState();

  const onToggle = useCallback(
    () => editor.setEditable(!editing),
    [editing, editor],
  );

  const onAction = useCallback(
    (action: ToolbarAction) => {
      switch (action.kind) {
        case 'wrap':
          return applyWrap(editor, action.before, action.after);
        case 'prefix':
          return applyPrefix(editor, action.prefix);
        case 'snippet':
          return applySnippet(editor, action.snippet);
      }
    },
    [editor],
  );

  const onMouseDown = useCallback(
    (event: SyntheticEvent, action: ToolbarAction) => {
      event.preventDefault();
      event.stopPropagation();

      editor.focus();
      onAction(action);
    },
    [editor, onAction],
  );

  useEffect(() => {
    return editor.registerCommand(
      KEY_DOWN_COMMAND,
      (event: KeyboardEvent) => {
        const matched = TOOLBAR_ACTIONS
          .filter(action => action !== 'separator')
          .find((action) => matchesShortcut(event, action.shortcut));

        if (matched) {
          event.preventDefault();
          event.stopPropagation();

          onAction(matched);
        }

        return !!matched;
      },
      COMMAND_PRIORITY_NORMAL,
    );
  }, [onAction]);

  return (
    <div
      role="toolbar"
      data-slot="editor-toolbar-container"
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
        onClick={onToggle}
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
