import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import { Alert, AlertTitle, AlertDescription } from 'konfigyr/components/ui/alert';

describe('components/ui/alert', () => {
  afterEach(() => cleanup());

  test('should render default alert with title and description', () => {
    render(
      <Alert>
        <AlertTitle data-testid="title">Alert title</AlertTitle>
        <AlertDescription data-testid="description">
          <p>Alert description</p>
        </AlertDescription>
      </Alert>,
    );

    expect(screen.getByRole('alert')).toBeDefined();
    expect(screen.getByTestId('title')).toBeDefined();
    expect(screen.getByTestId('title')).toHaveTextContent('Alert title');
    expect(screen.getByTestId('description')).toBeDefined();
    expect(screen.getByTestId('description')).toHaveTextContent('Alert description');
  });
});
