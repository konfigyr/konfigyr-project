import { afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import userEvents from '@testing-library/user-event';
import {
  InlineEdit,
  InlineEditInput,
  InlineEditPlaceholder,
  InlineEditSwitch,
  InlineEditTextarea,
} from '@konfigyr/components/ui/inline-edit';

describe('components | UI | <InlineEdit/>', () => {
  const onChange: (value?: any) => void = vi.fn();

  afterEach(() => {
    vi.resetAllMocks();
    cleanup();
  });

  test('should render inline edit with default placeholder showing the value argument', () => {
    const result = renderWithMessageProvider(
      <InlineEdit value="my value" onChange={onChange}>
        <InlineEditPlaceholder />
      </InlineEdit>,
    );

    const element = result.getByRole('button');
    expect(element).toBeInTheDocument();
    expect(element).toHaveTextContent('my value');
  });

  test('should render inline edit with custom placeholder', () => {
    const result = renderWithMessageProvider(
      <InlineEdit value="my value" onChange={onChange}>
        <InlineEditPlaceholder>
          <p>Customized placeholder</p>
        </InlineEditPlaceholder>
      </InlineEdit>,
    );

    const element = result.getByRole('button');
    expect(element).toBeInTheDocument();
    expect(element).toHaveTextContent('Customized placeholder');
  });

  test('should open editing state when placeholder is clicked', async () => {
    const result = renderWithMessageProvider(
      <InlineEdit value="my value" onChange={onChange}>
        <InlineEditPlaceholder />
        <InlineEditInput />
      </InlineEdit>,
    );

    expect(result.queryByRole('textbox')).toBeNull();

    await userEvents.click(result.getByRole('button'));

    const input = result.getByRole('textbox');
    expect(input).toBeInTheDocument();
    expect(input).toHaveFocus();
    expect(input).toHaveSelection('my value');
    expect(input).toHaveValue('my value');

    expect(result.getByRole('button', { name: 'Save' })).toBeInTheDocument();
    expect(result.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
  });

  test('should open editing state when placeholder is focused and Enter key is pressed', async () => {
    const result = renderWithMessageProvider(
      <InlineEdit value="my value" onChange={onChange}>
        <InlineEditPlaceholder />
        <InlineEditInput />
      </InlineEdit>,
    );

    expect(result.queryByRole('textbox')).toBeNull();

    await userEvents.tab();
    expect(result.getByRole('button')).toHaveFocus();

    await userEvents.keyboard('{Enter}');

    const input = result.getByRole('textbox');
    expect(input).toBeInTheDocument();
    expect(input).toHaveFocus();
    expect(input).toHaveSelection('my value');
    expect(input).toHaveValue('my value');

    expect(result.getByRole('button', { name: 'Save' })).toBeInTheDocument();
    expect(result.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
  });

  test('should exit editing state when cancel action button is clicked', async () => {
    const result = renderWithMessageProvider(
      <InlineEdit value="my value" onChange={onChange}>
        <InlineEditPlaceholder />
        <InlineEditInput />
      </InlineEdit>,
    );

    await userEvents.click(result.getByRole('button'));
    expect(result.queryByRole('textbox')).toBeInTheDocument();

    await userEvents.click(result.getByRole('button', { name: 'Cancel' }));
    expect(result.queryByRole('textbox')).toBeNull();

    expect(onChange).not.toBeCalled();
  });

  test('should exit editing state when escape key is pressed', async () => {
    const result = renderWithMessageProvider(
      <InlineEdit value="my value" onChange={onChange}>
        <InlineEditPlaceholder />
        <InlineEditInput />
      </InlineEdit>,
    );

    await userEvents.click(result.getByRole('button'));
    expect(result.queryByRole('textbox')).toBeInTheDocument();

    await userEvents.keyboard('{Escape}');
    expect(result.queryByRole('textbox')).toBeNull();

    expect(onChange).not.toBeCalled();
  });

  test('should render inline edit with input field', async () => {
    const result = renderWithMessageProvider(
      <InlineEdit value="my value" onChange={onChange}>
        <InlineEditPlaceholder />
        <InlineEditInput />
      </InlineEdit>,
    );

    await userEvents.click(result.getByRole('button'));
    expect(result.queryByRole('textbox')).toBeInTheDocument();

    await userEvents.clear(result.getByRole('textbox'));
    await userEvents.type(result.getByRole('textbox'), 'new value');

    await userEvents.keyboard('{Enter}');

    await waitFor(() => {
      expect(onChange).toBeCalledWith('new value');
    });
  });

  test('should render inline edit with textarea field', async () => {
    const result = renderWithMessageProvider(
      <InlineEdit value="text area value" onChange={onChange}>
        <InlineEditPlaceholder />
        <InlineEditTextarea />
      </InlineEdit>,
    );

    await userEvents.click(result.getByRole('button'));
    expect(result.queryByRole('textbox')).toBeInTheDocument();

    await userEvents.type(result.getByRole('textbox'), ', appended text');
    await userEvents.click(result.getByRole('button', { name: 'Save' }));

    expect(onChange).toBeCalledWith('text area value, appended text');
  });

  test('should render inline edit with switch field', async () => {
    const result = renderWithMessageProvider(
      <InlineEdit value={true} onChange={onChange}>
        <InlineEditPlaceholder />
        <InlineEditSwitch />
      </InlineEdit>,
    );

    await userEvents.click(result.getByRole('button'));
    expect(result.queryByRole('switch')).toBeInTheDocument();
    expect(result.queryByRole('switch')).toBeChecked();

    await userEvents.click(result.getByRole('switch'));
    expect(result.queryByRole('switch')).not.toBeChecked();

    await userEvents.click(result.getByRole('button', { name: 'Save' }));

    expect(onChange).toBeCalledWith(false);
  });
});
