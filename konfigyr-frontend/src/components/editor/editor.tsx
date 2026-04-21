import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import {
  BoldIcon,
  ItalicIcon,
  LinkIcon,
  ListChecksIcon,
  ListIcon,
  ListOrderedIcon,
  QuoteIcon,
  SquarePenIcon,
  StrikethroughIcon,
  ViewIcon,
} from 'lucide-react';
import {
  TextAreaCommandOrchestrator,
  commands,
  handleKeyDown,
  shortcuts,
} from '@uiw/react-md-editor';
import { cn } from '@konfigyr/components/utils';

import { MarkdownPreview } from './preview';
import { StatusBar } from './statusbar';
import { ToolbarPlugin } from './toolbar';

import type {
  ChangeEvent,
  ComponentProps,
  Dispatch,
  KeyboardEvent,
  ReactElement,
  ReactNode,
} from 'react';
import type { ICommand } from '@uiw/react-md-editor';

export type EditorProps = {
  value?: string;
  editing?: boolean;
  placeholder?: string;
  onValueChange?: Dispatch<string>;
  onEditingChange?: Dispatch<boolean>;
  children?: ReactNode;
} & Omit<ComponentProps<'textarea'>, 'onChange' | 'onKeyDown' | 'onError'>;

const createCommand = (parent: ICommand, icon: ReactElement): ICommand => ({
  ...parent,
  icon,
});

const COMMANDS: Array<ICommand> = [
  createCommand(commands.codeEdit, <SquarePenIcon />),
  createCommand(commands.codePreview, <ViewIcon />),
  commands.divider,
  createCommand(commands.bold, <BoldIcon />),
  createCommand(commands.italic, <ItalicIcon />),
  createCommand(commands.strikethrough, <StrikethroughIcon />),
  commands.divider,
  createCommand(commands.unorderedListCommand, <ListIcon />),
  createCommand(commands.orderedListCommand, <ListOrderedIcon />),
  createCommand(commands.quote, <QuoteIcon />),
  commands.divider,
  createCommand(commands.link, <LinkIcon />),
  createCommand(commands.checkedListCommand, <ListChecksIcon />),
];

export function Editor({
  name,
  editing,
  value,
  placeholder,
  onEditingChange,
  onValueChange,
  className,
  children,
  ...props
}: EditorProps) {
  const [internalValue, onInternalValueChange] = useState(value ?? '');
  const [internalEditing, onInternalEditingChange] = useState(editing ?? true);

  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const orchestratorRef = useRef<TextAreaCommandOrchestrator | null>(null);

  const markdown = useMemo(() => value ?? internalValue, [value, internalValue]);
  const isEditing = useMemo(() => editing ?? internalEditing, [editing, internalEditing]);

  // Wire up the orchestrator once the textarea is mounted
  useEffect(() => {
    if (textareaRef.current) {
      orchestratorRef.current = new TextAreaCommandOrchestrator(textareaRef.current);
    }
  }, []);

  const onExecuteCommand = useCallback((command: ICommand<any>) => {
    if (orchestratorRef.current) {
      orchestratorRef.current.executeCommand(command);
    }
  }, []);

  const onKeyDown = useCallback((e: KeyboardEvent<HTMLTextAreaElement>) => {
    handleKeyDown(e.nativeEvent, 2, false);
    if (orchestratorRef.current) {
      shortcuts(e, COMMANDS, orchestratorRef.current);
    }
  }, []);

  const handleEditingChange = useCallback(
    (nextState: boolean) => {
      onInternalEditingChange(nextState);
      onEditingChange?.(nextState);
    },
    [onEditingChange],
  );

  const handleValueChange = useCallback(
    (e: ChangeEvent<HTMLTextAreaElement>) => {
      onInternalValueChange(e.target.value);
      onValueChange?.(e.target.value);
    },
    [onValueChange],
  );

  return (
    <div
      data-slot="editor"
      data-editing={editing}
      className={cn('grid gap-1', className)}
    >
      <div
        data-slot="editor-container"
        className="rounded-xl border border-input transition-[color,box-shadow] focus-within:border-ring focus-within:ring-3 focus-within:ring-ring/50"
      >
        <ToolbarPlugin
          editing={isEditing}
          onEditing={handleEditingChange}
          onCommand={onExecuteCommand}
        />

        {isEditing ? (
          <textarea
            value={markdown}
            onChange={handleValueChange}
            onKeyDown={onKeyDown}
            placeholder={placeholder}
            className="group/editor min-h-36 max-h-120 w-full overflow-y-auto px-2.5 py-1 bg-transparent text-sm outline-none leading-relaxed text-foreground whitespace-pre-wrap"
            spellCheck
            {...props}
            ref={textareaRef}
          />
        ) : (
          <MarkdownPreview value={markdown} />
        )}
      </div>

      <StatusBar value={markdown} />
    </div>
  );
}
