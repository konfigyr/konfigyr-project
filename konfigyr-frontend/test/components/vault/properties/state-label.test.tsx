import { afterEach, describe, expect, test } from 'vitest';
import { cleanup } from '@testing-library/react';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import { StateBadge, StateLabel } from '@konfigyr/components/vault/properties/state-label';

describe('components | vault | properties | <StateLabel/>', () => {
  afterEach(() => cleanup());

  test('should render added variant property state label', () => {
    const result = renderWithMessageProvider(
      <StateLabel variant="added" />,
    );

    const element = result.getByRole('paragraph');
    expect(element).toBeInTheDocument();
    expect(element).toHaveTextContent('added');
    expect(element).toHaveClass('[&>*:first-child]:bg-emerald-600');
  });

  test('should render modified variant property state label with count', () => {
    const result = renderWithMessageProvider(
      <StateLabel variant="modified" count={12} />,
    );

    const element = result.getByRole('paragraph');
    expect(element).toBeInTheDocument();
    expect(element).toHaveTextContent('12modified');
    expect(element).toHaveClass('[&>*:first-child]:bg-amber-600');
  });

  test('should render deleted deprecated property state label', () => {
    const result = renderWithMessageProvider(
      <StateLabel variant="deprecated" />,
    );

    const element = result.getByRole('paragraph');
    expect(element).toBeInTheDocument();
    expect(element).toHaveTextContent('deprecated');
    expect(element).toHaveClass('[&>*:first-child]:bg-destructive');
  });

  test('should render deleted variant property state label with zero count', () => {
    const result = renderWithMessageProvider(
      <StateLabel variant="deleted" count={0} />,
    );

    const element = result.getByRole('paragraph');
    expect(element).toBeInTheDocument();
    expect(element).toHaveTextContent('deleted');
    expect(element).toHaveClass('[&>*:first-child]:bg-destructive');
  });
});

describe('components | vault | properties | <StateBadge/>', () => {
  afterEach(() => cleanup());

  test('should render added variant property state badge', () => {
    const result = renderWithMessageProvider(
      <StateBadge variant="added" data-testid="badge" />,
    );

    const element = result.getByTestId('badge');
    expect(element).toBeInTheDocument();
    expect(element).toHaveTextContent('added');
    expect(element).toHaveClass('border-emerald-400/40', 'text-emerald-600', 'dark:text-emerald-400');
  });

  test('should render modified variant property state badge', () => {
    const result = renderWithMessageProvider(
      <StateBadge variant="modified" data-testid="badge" />,
    );

    const element = result.getByTestId('badge');
    expect(element).toBeInTheDocument();
    expect(element).toHaveTextContent('modified');
    expect(element).toHaveClass('border-amber-400/40', 'text-amber-600', 'dark:text-amber-400');
  });

  test('should render deleted deprecated property state badge', () => {
    const result = renderWithMessageProvider(
      <StateBadge variant="deprecated" data-testid="badge" />,
    );

    const element = result.getByTestId('badge');
    expect(element).toBeInTheDocument();
    expect(element).toHaveTextContent('deprecated');
    expect(element).toHaveClass('border-destructive/40', 'text-destructive');
  });

  test('should render deleted variant property state badge', () => {
    const result = renderWithMessageProvider(
      <StateBadge variant="deleted" data-testid="badge" />,
    );

    const element = result.getByTestId('badge');
    expect(element).toBeInTheDocument();
    expect(element).toHaveTextContent('deleted');
    expect(element).toHaveClass('border-destructive/40', 'text-destructive');
  });
});
