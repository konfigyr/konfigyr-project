import { afterEach, describe, expect, test } from 'vitest';
import { Avatar } from '@konfigyr/components/ui/avatar';
import { cleanup, render, screen } from '@testing-library/react';

describe('components | UI | <Avatar/>', () => {
  afterEach(() => cleanup());

  test('should render default avatar size', () => {
    render(
      <Avatar data-testid="avatar" />,
    );

    expect(screen.getByTestId('avatar')).toBeInTheDocument();
    expect(screen.getByTestId('avatar')).toHaveClass('size-8');
  });

  test('should render small avatar size', () => {
    render(
      <Avatar size="sm" data-testid="avatar" />,
    );

    expect(screen.getByTestId('avatar')).toBeInTheDocument();
    expect(screen.getByTestId('avatar')).toHaveClass('size-6');
  });

  test('should render large avatar size', () => {
    render(
      <Avatar size="lg" data-testid="avatar" />,
    );

    expect(screen.getByTestId('avatar')).toBeInTheDocument();
    expect(screen.getByTestId('avatar')).toHaveClass('size-12');
  });

  test('should render extra large avatar size', () => {
    render(
      <Avatar size="xl" data-testid="avatar" />,
    );

    expect(screen.getByTestId('avatar')).toBeInTheDocument();
    expect(screen.getByTestId('avatar')).toHaveClass('size-16');
  });
});
