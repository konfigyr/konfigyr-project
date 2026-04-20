import { useIntl } from 'react-intl';

import { ContentEditable } from '@lexical/react/LexicalContentEditable';
import { LexicalErrorBoundary } from '@lexical/react/LexicalErrorBoundary';
import { PlainTextPlugin } from '@lexical/react/LexicalPlainTextPlugin';
import { useEditorState } from './context';

import type { ReactNode } from 'react';
import type { ContentEditableProps } from '@lexical/react/LexicalContentEditable';

export type MarkdownEditorProps = {
  label?: string;
  placeholder?: ReactNode;
} & Omit<ContentEditableProps, 'placeholder' | 'aria-placeholder'>;

function EditorPlaceholder({ placeholder }: { placeholder?: ReactNode }) {
  const intl = useIntl();

  if (placeholder === undefined) {
    placeholder = intl.formatMessage({
      defaultMessage: 'Start typing...',
      description: 'Default placeholder text for the editor',
    });
  }

  return (
    <span className="absolute top-1.5 left-2.5 text-sm text-muted-foreground pointer-events-none select-none">
      {placeholder}
    </span>
  );
}

export function MarkdownEditor({ label, placeholder, ...props }: MarkdownEditorProps) {
  const { value } = useEditorState();

  return (
    <PlainTextPlugin
      data-slot="editor-content"
      contentEditable={
        <ContentEditable
          value={value}
          className="group/editor min-h-22 overflow-y-auto px-2.5 py-1 bg-transparent text-sm outline-none leading-relaxed text-foreground whitespace-pre-wrap resize-none"
          aria-label={label}
          aria-multiline="true"
          role="textbox"
          spellCheck
          {...props}
        />
      }
      placeholder={<EditorPlaceholder placeholder={placeholder} />}
      ErrorBoundary={LexicalErrorBoundary}
    />
  );
}
