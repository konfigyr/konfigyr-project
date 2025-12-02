import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, render } from '@testing-library/react';
import { Input } from '@konfigyr/components/ui/input';

describe('components | UI | <Input/>', () => {
  afterEach(() => cleanup());

  test('should render input field', () => {
    const { getByTestId } = render(
      <Input data-testid="input" name="test" type="number" value={1234} />,
    );

    const input = getByTestId('input');
    expect(input).toBeInTheDocument();
    expect(input).toHaveValue(1234);
    expect(input).toHaveAttribute('name', 'test');
    expect(input).toHaveAttribute('type', 'number');
  });
});
