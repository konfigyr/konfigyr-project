import { useCallback, useEffect, useLayoutEffect } from 'react';
import {
  $createParagraphNode,
  $createTextNode,
  $getRoot,
  $getSelection,
  $insertNodes,
  $isRangeSelection,
  COMMAND_PRIORITY_NORMAL,
  KEY_TAB_COMMAND,
} from 'lexical';
import { useLexicalComposerContext } from '@lexical/react/LexicalComposerContext';
import { OnChangePlugin } from '@lexical/react/LexicalOnChangePlugin';
import { useEditorState } from './context';

import type { Dispatch } from 'react';
import type { EditorState } from 'lexical';

export function EditorListenerPlugin({ onEditingChange, onValueChange }: {
  onValueChange?: Dispatch<string>;
  onEditingChange?: Dispatch<boolean>;
}) {
  const [editor] = useLexicalComposerContext();
  const state = useEditorState();

  useLayoutEffect(
    () => editor.registerEditableListener(editable => {
      state.onEditingChange(editable);
      onEditingChange?.(editable);
    }),
    [editor, state.onEditingChange, onEditingChange],
  );

  useEffect(() => {
    let currentEditorText = '';
    editor.getEditorState().read(() => {
      currentEditorText = $getRoot().getTextContent();
    });

    if (state.value === currentEditorText) {
      return;
    }

    editor.update(() => {
      const root = $getRoot();
      root.clear();

      const lines = state.value.split('\n');
      lines.forEach((line) => {
        const paragraph = $createParagraphNode();
        paragraph.append($createTextNode(line));
        root.append(paragraph);
      });
    });
  }, [editor, state.value]);

  const onChange = useCallback((editorState: EditorState) => {
    editorState.read(() => {
      const contents = $getRoot().getTextContent();

      if (contents === state.value) {
        return;
      }

      state.onValueChange(contents);
      onValueChange?.(contents);
    });
  }, [editor, state.onValueChange, onValueChange]);

  return (
    <OnChangePlugin
      onChange={onChange}
      ignoreSelectionChange={true}
    />
  );
}

export function TabPlugin({ spacing = 2 }: { spacing?: number }) {
  const [editor] = useLexicalComposerContext();

  useLayoutEffect(() => {
    return editor.registerCommand(
      KEY_TAB_COMMAND,
      (event: KeyboardEvent) => {
        event.preventDefault();

        editor.update(() => {
          const selection = $getSelection();

          if ($isRangeSelection(selection)) {
            $insertNodes([$createTextNode(' '.repeat(spacing))]);
          }
        });

        return true;
      },
      COMMAND_PRIORITY_NORMAL,
    );
  }, [editor]);

  return null;
}
