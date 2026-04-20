import { createContext, useContext, useState } from 'react';

import { useErrorNotification } from '@konfigyr/components/error';
import { LexicalComposer } from '@lexical/react/LexicalComposer';

import type { Dispatch, ReactNode } from 'react';
import type { LexicalEditor } from 'lexical';
import type { InitialConfigType } from '@lexical/react/LexicalComposer';

export interface EditorState {
  value: string;
  editing: boolean;
  onValueChange: Dispatch<string>;
  onEditingChange: Dispatch<boolean>;
}

export interface EditorStateProviderProps {
  name: string,
  value?: string;
  editing?: boolean;
  children: ReactNode,
  onError?: (error: Error, editor: LexicalEditor) => void;
}

const EditorStateContext = createContext<EditorState>({
  value: '',
  editing: false,
  onEditingChange: () => {},
  onValueChange: () => {},
});

export function useEditorState() {
  return useContext(EditorStateContext);
}

export function EditorStateProvider(props: EditorStateProviderProps) {
  const [value, setValue] = useState(props.value ?? '');
  const [editing, setEditing] = useState(props.editing ?? true);
  const errorNotification = useErrorNotification();

  const configuration: InitialConfigType = {
    namespace: props.name,
    onError: props.onError ?? errorNotification,
  };

  const context: EditorState = {
    editing: props.editing ?? editing,
    value: props.value ?? value,
    onEditingChange: setEditing,
    onValueChange: setValue,
  };

  return (
    <EditorStateContext.Provider value={context}>
      <LexicalComposer initialConfig={configuration}>
        {props.children}
      </LexicalComposer>
    </EditorStateContext.Provider>
  );
}
