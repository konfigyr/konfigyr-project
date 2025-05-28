import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import { Card, CardHeader, CardContent, CardFooter, CardAction } from 'konfigyr/components/ui/card';

describe('components/ui/card', () => {
  afterEach(() => cleanup());

  test('should render default alert with title and description', () => {
    render(
      <Card data-testid="container">
        <CardHeader title="Card title" description="Card description" />
        <CardContent>
          <p>Card contents</p>
        </CardContent>
        <CardFooter>
          <CardAction>
            <a href="#">Card action</a>
          </CardAction>
        </CardFooter>
      </Card>,
    );

    expect(screen.getByTestId('container')).toBeDefined();
    expect(screen.getByText('Card title')).toBeDefined();
    expect(screen.getByText('Card description')).toBeDefined();
    expect(screen.getByRole('paragraph')).toBeDefined();
    expect(screen.getByRole('paragraph')).toHaveTextContent('Card contents');
    expect(screen.getByRole('link')).toBeDefined();
    expect(screen.getByRole('link')).toHaveTextContent('Card action');
  });
});
