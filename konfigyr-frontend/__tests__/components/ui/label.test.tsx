import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import { Label } from 'konfigyr/components/ui/label';

describe('components/ui/label', () => {
  afterEach(() => cleanup());

  test('should render label for input field', () => {
    render(
      <>
        <Label data-testid="label" htmlFor="input">Enter value</Label>
        <input data-testid="input" id="input" type="text" />
      </>,
    );

    const label = screen.getByTestId('label');
    expect(label).toBeInTheDocument();
    expect(label).toHaveTextContent('Enter value');
    expect(label).toHaveAttribute('for', 'input');
  });
});
