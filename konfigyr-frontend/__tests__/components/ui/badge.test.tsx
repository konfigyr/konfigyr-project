import { afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { Badge } from 'konfigyr/components/ui/badge';
import { Button } from 'konfigyr/components/ui/button';

describe('components/ui/badge', () => {
  afterEach(() => cleanup());

  test('should render default badge variant', () => {
    render(<Badge>Default badge</Badge>);

    const badge = screen.getByText('Default badge');
    expect(badge).toBeDefined();
    expect(badge).toBeInstanceOf(HTMLSpanElement);
    expect(badge).toHaveClass('bg-primary text-primary-foreground');
  });

  test('should render secondary badge variant as link', () => {
    render(<Badge variant="secondary" asChild><a href="https://example.com">Link</a></Badge>);

    const badge = screen.getByRole('link');
    expect(badge).toHaveTextContent('Link');
    expect(badge).toHaveClass('bg-secondary text-secondary-foreground');
  });

  test('should render destructive badge variant', () => {
    render(<Badge variant="destructive">Destructive badge</Badge>);

    const badge = screen.getByText('Destructive badge');
    expect(badge).toBeDefined();
    expect(badge).toHaveClass('bg-destructive text-white');
  });

  test('should render outline badge variant as button', () => {
    const handler = vi.fn();

    render(
      <Badge variant="outline" onClick={handler} asChild>
        <Button>Button outline badge</Button>
      </Badge>,
    );

    const badge = screen.getByRole('button');
    expect(badge).toBeDefined();
    expect(badge).toHaveTextContent('Button outline badge');
    expect(badge).toHaveClass('text-foreground');

    fireEvent.click(badge);

    expect(handler).toHaveBeenCalled();
  });

});
