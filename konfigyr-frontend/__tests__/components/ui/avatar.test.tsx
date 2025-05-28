import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import { Avatar } from 'konfigyr/components/ui/avatar';

describe('components/ui/avatar', () => {
  afterEach(() => cleanup());

  test('should render default avatar size', async () => {
    render(
      <Avatar data-testid="avatar" />,
    );

    expect(screen.getByTestId('avatar')).toBeDefined();
    expect(screen.getByTestId('avatar')).toHaveClass('size-8');
  });

  test('should render small avatar size', async () => {
    render(
      <Avatar size="sm" data-testid="avatar" />,
    );

    expect(screen.getByTestId('avatar')).toBeDefined();
    expect(screen.getByTestId('avatar')).toHaveClass('size-6');
  });

  test('should render large avatar size', async () => {
    render(
      <Avatar size="lg" data-testid="avatar" />,
    );

    expect(screen.getByTestId('avatar')).toBeDefined();
    expect(screen.getByTestId('avatar')).toHaveClass('size-12');
  });
});
