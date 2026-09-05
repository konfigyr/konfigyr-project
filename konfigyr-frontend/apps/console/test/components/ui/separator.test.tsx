import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, render } from '@testing-library/react';
import { Separator } from '@konfigyr/components/ui/separator';

describe('components | UI | <Separator/>', () => {
  afterEach(() => cleanup());

  test('should render separator with default orientation', () => {
    const { getByTestId } = render(<Separator data-testid="separator" />);

    const input = getByTestId('separator');
    expect(input).toBeInTheDocument();
    expect(input).toHaveAttribute('data-orientation', 'horizontal');
    expect(input).toHaveAttribute('aria-orientation', 'horizontal');
  });

  test('should render separator with vertical orientation', () => {
    const { getByTestId } = render(<Separator data-testid="separator" orientation="vertical" />);

    const input = getByTestId('separator');
    expect(input).toBeInTheDocument();
    expect(input).toHaveAttribute('data-orientation', 'vertical');
    expect(input).toHaveAttribute('aria-orientation', 'vertical');
  });

  test('should render separator with aria-orientation', () => {
    const { getByTestId } = render(
      <Separator
        data-testid="separator"
        orientation="vertical"
      />,
    );

    const input = getByTestId('separator');
    expect(input).toBeInTheDocument();
    expect(input).toHaveAttribute('data-orientation', 'vertical');
    expect(input).toHaveAttribute('aria-orientation', 'vertical');
  });
});
