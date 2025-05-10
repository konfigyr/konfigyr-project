import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import ServiceLabel from 'konfigyr/components/service-label';

describe('components/service-label', () => {
  afterEach(() => cleanup());

  test('should render default service label', () => {
    render(<ServiceLabel />);
    expect(screen.getByText('konfigyr')).toBeDefined();
    expect(screen.getByText('vault')).toBeDefined();
  });

  test('should render with service label', () => {
    render(<ServiceLabel name="custom-label" />);
    expect(screen.getByText('konfigyr')).toBeDefined();
    expect(screen.getByText('custom-label')).toBeDefined();
  });
});
