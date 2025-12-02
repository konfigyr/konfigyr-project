import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, render } from '@testing-library/react';
import { Textarea } from '@konfigyr/components/ui/textarea';

describe('components | UI | <Textarea/>', () => {
  afterEach(() => cleanup());

  test('should render textarea field', () => {
    const { getByTestId } = render(
      <Textarea data-testid="input" name="test" rows={6}>
        Some text value
      </Textarea>,
    );

    const input = getByTestId('input');
    expect(input).toBeInTheDocument();
    expect(input).toHaveValue('Some text value');
    expect(input).toHaveAttribute('name', 'test');
    expect(input).toHaveAttribute('rows', '6');
  });
});
