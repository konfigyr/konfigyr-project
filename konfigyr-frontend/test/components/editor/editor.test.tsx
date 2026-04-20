import { afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import { Editor } from '@konfigyr/components/editor';

describe('components | clipboard | <Editor/>', () => {
  afterEach(() => cleanup());

  test('renders the editor region with the correct ARIA roles', () => {
    const { getByRole } = renderWithMessageProvider(
      <Editor name="test-editor" aria-label="comments"/>,
    );

    expect(getByRole('textbox')).toBeInTheDocument();
    expect(getByRole('textbox')).toHaveAccessibleName('comments');

    expect(getByRole('toolbar')).toBeInTheDocument();

    expect(getByRole('button', { name: 'Preview' })).toBeInTheDocument();
  });

  test('renders the placeholder when no value is provided', () => {
    const { getByText } = renderWithMessageProvider(
      <Editor name="test-editor" placeholder={<span>Start writing...</span>}/>,
    );
    expect(getByText('Start writing...')).toBeInTheDocument();
  });

  test('renders with an initial value visible in the editor', async () => {
    const { queryByText, getByRole } = renderWithMessageProvider(
      <Editor name="test-editor" placeholder="Start writing" value="Hello world"/>,
    );

    await waitFor(() => {
      expect(getByRole('textbox')).toHaveTextContent('Hello world');
    });

    expect(queryByText('Start writing')).not.toBeInTheDocument();
  });

  /*
  see this issue: https://github.com/facebook/lexical/discussions/2659

  test('displays typed text in the editor region', async () => {
    const { getByRole } = renderWithMessageProvider(
      <Editor name="test-editor" />,
    );

    const editor = getByRole('textbox');

    await userEvent.click(editor);
    await userEvent.keyboard('Hello world from the editor');

    await waitFor(() => {
      expect(editor).toHaveTextContent('Hello world from the editor');
    });
  });

  test('calls onValueChange when the editor contents is updated', async () => {
    const onValueChange = vi.fn();

    const { getByRole } = renderWithMessageProvider(
      <Editor name="test-editor" onValueChange={onValueChange} />,
    );

    const editor = getByRole('textbox');

    expect(onValueChange).not.toHaveBeenCalled();

    await userEvent.click(editor);
    await userEvent.type(editor, 'Hello');

    await waitFor(() => {
      expect(onValueChange).toHaveBeenCalled();
    });

    expect(onValueChange).toHaveBeenLastCalledWith('Hello');
  });
  */

  test('should update the controller value prop in the editor', async () => {
    const { rerender, getByRole } = renderWithMessageProvider(
      <Editor name="test-editor" value="Initial contents"/>,
    );

    await waitFor(() => {
      expect(getByRole('textbox')).toHaveTextContent('Initial contents');
    });

    rerender(
      <Editor name="test-editor" value="Updated contents"/>,
    );

    await waitFor(() => {
      expect(getByRole('textbox')).toHaveTextContent('Updated contents');
    });
  });

  test('clears the editor when value is reset to an empty string', async () => {
    const { rerender, getByRole } = renderWithMessageProvider(
      <Editor name="test-editor" value="Initial contents" placeholder="Placeholder text"/>,
    );

    await waitFor(() => {
      expect(getByRole('textbox')).toHaveTextContent('Initial contents');
    });

    rerender(
      <Editor name="test-editor" value=""/>,
    );

    await waitFor(() => {
      expect(getByRole('textbox')).toHaveTextContent('');
    });
  });

  test('does not re-render the editor when the same value is passed again', async () => {
    const onValueChange = vi.fn();

    const { rerender, getByRole } = renderWithMessageProvider(
      <Editor name="test-editor" value="Initial contents" onValueChange={onValueChange}/>,
    );

    await waitFor(() => {
      expect(getByRole('textbox')).toHaveTextContent('Initial contents');
    });

    rerender(
      <Editor name="test-editor" value="Initial contents" onValueChange={onValueChange}/>,
    );

    await waitFor(() => {
      expect(onValueChange).not.toHaveBeenCalled();
    });
  });

  test('switches to the preview when the preview button is clicked', async () => {
    const onEditingChange = vi.fn();
    const { queryByRole, getByRole } = renderWithMessageProvider(
      <Editor name="test-editor" onEditingChange={onEditingChange} />,
    );

    expect(queryByRole('button', { name: 'Preview' })).toBeInTheDocument();

    await userEvent.click(
      getByRole('button', { name: 'Preview' }),
    );

    await waitFor(() => {
      expect(queryByRole('button', { name: 'Preview' })).not.toBeInTheDocument();
    });

    expect(queryByRole('button', { name: 'Edit' })).toBeInTheDocument();
    expect(onEditingChange).toHaveBeenCalledExactlyOnceWith(false);
  });

  test('renders the markdown as HTML in the preview panel', async () => {
    const { getByRole } = renderWithMessageProvider(<Editor name="test-editor" value="# Hello world"/>);

    await userEvent.click(
      getByRole('button', { name: 'Preview' }),
    );

    await waitFor(() => {
      expect(getByRole('note')).toBeInTheDocument();
    });

    expect(getByRole('note').querySelector('h1')).toHaveTextContent('Hello world');
  });

  test('renders bold markdown correctly in preview', async () => {
    const { getByRole } = renderWithMessageProvider(<Editor name="test-editor" value="**bold text**"/>);

    await userEvent.click(
      getByRole('button', { name: 'Preview' }),
    );

    await waitFor(() => {
      expect(getByRole('note')).toBeInTheDocument();
    });

    expect(getByRole('note').querySelector('strong')).toHaveTextContent('bold text');
  });

  test('renders bullet list correctly in preview', async () => {
    const { getByRole } = renderWithMessageProvider(
      <Editor name="test-editor" value={'- item one\n- item two'}/>,
    );

    await userEvent.click(
      getByRole('button', { name: 'Preview' }),
    );

    await waitFor(() => {
      expect(getByRole('note')).toBeInTheDocument();
    });

    const items = getByRole('note').querySelectorAll('li');
    expect(items).toHaveLength(2);
    expect(items[0]).toHaveTextContent('item one');
    expect(items[1]).toHaveTextContent('item two');
  });
});
