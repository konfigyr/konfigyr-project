import { afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, fireEvent, render } from '@testing-library/react';
import { Button } from '@konfigyr/components/ui/button';

describe('components | UI | <Button/>', () => {
  afterEach(() => cleanup());

  test('should render default button with child elements', () => {
    const { getByRole } = render(<Button>Default button</Button>);

    expect(getByRole('button')).toBeDefined();
    expect(getByRole('button')).toHaveTextContent('Default button');
    expect(getByRole('button')).toHaveClass('bg-primary text-primary-foreground h-9');
  });

  test('should render ghost button with click action handler', () => {
    const handler = vi.fn();

    const { getByRole } = render(<Button variant="ghost" onClick={handler}>Ghost button</Button>);

    expect(getByRole('button')).toBeDefined();
    expect(getByRole('button')).toHaveClass('hover:bg-muted hover:text-foreground');

    fireEvent.click(getByRole('button'));

    expect(handler).toHaveBeenCalled();
  });

  test('should render button with custom element', () => {
    const { getByRole } = render(<Button variant="link" render={<a href="https://example.com">Link</a>} />);

    expect(getByRole('link')).toBeDefined();
    expect(getByRole('link')).toHaveTextContent('Link');
    expect(getByRole('link')).toHaveClass('text-primary underline-offset-4 hover:underline');
  });

  test('should render small destructive button', () => {
    const { getByRole } = render(<Button variant="destructive" size="sm">Small</Button>);

    expect(getByRole('button')).toBeDefined();
    expect(getByRole('button')).toHaveClass('bg-destructive/10 text-destructive h-7');
  });

  test('should render large outline button', () => {
    const { getByRole } = render(<Button variant="outline" size="lg">Large</Button>);

    expect(getByRole('button')).toBeDefined();
    expect(getByRole('button')).toHaveClass('bg-background h-9');
  });

  test('should render secondary icon button', () => {
    const { getByRole } = render(<Button variant="secondary" size="icon">Icon</Button>);

    expect(getByRole('button')).toBeDefined();
    expect(getByRole('button')).toHaveClass('bg-secondary text-secondary-foreground size-8');
  });

  test('should render extra small icon button', () => {
    const { getByRole } = render(<Button variant="secondary" size="icon-xs">Icon</Button>);

    expect(getByRole('button')).toBeDefined();
    expect(getByRole('button')).toHaveClass('size-6');
  });

  test('should render small icon button', () => {
    const { getByRole } = render(<Button variant="secondary" size="icon-sm">Icon</Button>);

    expect(getByRole('button')).toBeDefined();
    expect(getByRole('button')).toHaveClass('size-7');
  });

  test('should render large icon button', () => {
    const { getByRole } = render(<Button variant="secondary" size="icon-lg">Icon</Button>);

    expect(getByRole('button')).toBeDefined();
    expect(getByRole('button')).toHaveClass('size-9');
  });
});
