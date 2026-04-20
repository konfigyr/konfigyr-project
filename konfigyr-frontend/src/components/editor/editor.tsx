import { cn } from '@konfigyr/components/utils';

import { AutoFocusPlugin } from '@lexical/react/LexicalAutoFocusPlugin';
import { HistoryPlugin } from '@lexical/react/LexicalHistoryPlugin';

import { EditorStateProvider, useEditorState } from './context';
import { MarkdownEditor } from './markdown-editor';
import { MarkdownPreview } from './markdown-preview';
import { EditorListenerPlugin, TabPlugin } from './plugins';
import { StatusBar } from './statusbar';
import { ToolbarPlugin } from './toolbar';

import type { ComponentProps, Dispatch, ReactNode } from 'react';
import type { LexicalEditor } from 'lexical';
import type { EditorStateProviderProps } from './context';

export function EditorContent({ placeholder, className, ...props }: { className?: string, placeholder: ReactNode } & ComponentProps<'div'>) {
  const { editing } = useEditorState();

  return (
    <div
      data-slot="editor-content-container"
      data-editing={editing}
      className={cn('relative overflow-hidden', className)}
      {...props}
    >
      {editing ? (
        <MarkdownEditor placeholder={placeholder} {...props} />
      ) : (
        <MarkdownPreview />
      )}
    </div>
  );
}

export type EditorProps = {
  placeholder?: ReactNode;
  onValueChange?: Dispatch<string>;
  onEditingChange?: Dispatch<boolean>;
  onError?: (error: Error, editor: LexicalEditor) => void;
  children?: ReactNode;
} & Omit<EditorStateProviderProps, 'children'> & Omit<ComponentProps<'div'>, 'onError'>;

export function Editor({
  name,
  editing,
  value,
  onError,
  placeholder,
  onEditingChange,
  onValueChange,
  className,
  children,
  ...props
}: EditorProps) {
  return (
    <EditorStateProvider
      name={name}
      value={value}
      editing={editing}
      onError={onError}
    >
      <div
        data-slot="editor"
        className={cn('grid gap-2', className)}
      >
        <div
          data-slot="editor-container"
          className="rounded-md border border-input transition-[color,box-shadow] focus-within:border-ring focus-within:ring-3 focus-within:ring-ring/50"
        >
          <ToolbarPlugin />

          <EditorContent placeholder={placeholder} {...props} />
        </div>

        <StatusBar />

        {children}
      </div>

      <EditorListenerPlugin
        onValueChange={onValueChange}
        onEditingChange={onEditingChange}
      />

      <TabPlugin />
      <HistoryPlugin />
      <AutoFocusPlugin />
    </EditorStateProvider>
  );
}
