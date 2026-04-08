import { afterEach, describe, expect, test } from 'vitest';
import { cleanup } from '@testing-library/react';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import { ConfigurationPropertyState } from '@konfigyr/hooks/vault/types';
import { StateBadge } from '@konfigyr/components/vault/properties/state-badge';

describe('components | vault | properties | <StateBadge/>', () => {
  afterEach(() => cleanup());

  test('should not render any state badge for unchanged state', () => {
    const result = renderWithMessageProvider(
      <StateBadge variant={ConfigurationPropertyState.UNCHANGED} data-testid="badge" />,
    );

    expect(result.queryByTestId('badge')).not.toBeInTheDocument();
  });

  test('should render added variant property state badge', () => {
    const result = renderWithMessageProvider(
      <StateBadge variant={ConfigurationPropertyState.ADDED} data-testid="badge" />,
    );

    const element = result.getByTestId('badge');
    expect(element).toBeInTheDocument();
    expect(element).toHaveTextContent('Added');
    expect(element).toHaveClass('border-emerald-400/40', 'text-emerald-600', 'dark:text-emerald-400');
  });

  test('should render modified variant property state badge', () => {
    const result = renderWithMessageProvider(
      <StateBadge variant={ConfigurationPropertyState.UPDATED} data-testid="badge" />,
    );

    const element = result.getByTestId('badge');
    expect(element).toBeInTheDocument();
    expect(element).toHaveTextContent('Modified');
    expect(element).toHaveClass('border-amber-400/40', 'text-amber-600', 'dark:text-amber-400');
  });

  test('should render deleted variant property state badge', () => {
    const result = renderWithMessageProvider(
      <StateBadge variant={ConfigurationPropertyState.REMOVED} data-testid="badge" />,
    );

    const element = result.getByTestId('badge');
    expect(element).toBeInTheDocument();
    expect(element).toHaveTextContent('Deleted');
    expect(element).toHaveClass('border-destructive/40', 'text-destructive');
  });
});
