import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, render } from '@testing-library/react';
import {
  InputGroup,
  InputGroupAddon,
  InputGroupButton,
  InputGroupInput,
  InputGroupText,
  InputGroupTextarea,
} from '@konfigyr/components/ui/input-group';

describe('components | UI | <InputGroup/>', () => {
  afterEach(() => cleanup());

  test('should render input group with input field and button', () => {
    const { getByTestId } = render(
      <InputGroup>
        <InputGroupInput data-testid="input" placeholder="Type to search..." />
        <InputGroupAddon align="inline-end">
          <InputGroupButton data-testid="button" variant="secondary">Search</InputGroupButton>
        </InputGroupAddon>
      </InputGroup>,
    );

    expect(getByTestId('input')).toBeInTheDocument();
    expect(getByTestId('button')).toBeInTheDocument();
  });

  test('should render input group with textarea with custom text', () => {
    const { getByTestId } = render(
      <InputGroup>
        <InputGroupTextarea data-testid="textarea" />
        <InputGroupAddon align="block-end" className="border-t">
          <InputGroupText data-testid="text">Maximum length: 500</InputGroupText>
        </InputGroupAddon>
      </InputGroup>,
    );

    expect(getByTestId('textarea')).toBeInTheDocument();
    expect(getByTestId('text')).toBeInTheDocument();
  });
});
