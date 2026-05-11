import { afterEach, describe, expect, test } from 'vitest';
import { cleanup } from '@testing-library/react';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import { AuditEntityTypeIcon } from '@konfigyr/components/audit/audit-entity-type';

describe('components | audit | <AuditEntityTypeIcon/>', () => {
  afterEach(() => cleanup());

  test('should render <AuditEntityTypeIcon/> component with a label for namespace entity type', () => {
    const { getByTestId } = renderWithMessageProvider(
      <AuditEntityTypeIcon value="namespace" data-testid="icon"/>,
    );

    expect(getByTestId('icon')).toBeInTheDocument();
    expect(getByTestId('icon')).toHaveAccessibleName('Namespace');
    expect(getByTestId('icon')).toHaveClass('lucide-folder-kanban');
  });

  test('should render <AuditEntityTypeIcon/> component with a label for namespace application entity type', () => {
    const { getByTestId } = renderWithMessageProvider(
      <AuditEntityTypeIcon value="namespace-application" data-testid="icon"/>,
    );

    expect(getByTestId('icon')).toBeInTheDocument();
    expect(getByTestId('icon')).toHaveAccessibleName('Application');
    expect(getByTestId('icon')).toHaveClass('lucide-monitor-cloud');
  });

  test('should render <AuditEntityTypeIcon/> component with a label for service entity type', () => {
    const { getByTestId } = renderWithMessageProvider(
      <AuditEntityTypeIcon value="service" data-testid="icon"/>,
    );

    expect(getByTestId('icon')).toBeInTheDocument();
    expect(getByTestId('icon')).toHaveAccessibleName('Service');
    expect(getByTestId('icon')).toHaveClass('lucide-app-window-mac');
  });

  test('should render <AuditEntityTypeIcon/> component with a label for keyset entity type', () => {
    const { getByTestId } = renderWithMessageProvider(
      <AuditEntityTypeIcon value="keyset" data-testid="icon"/>,
    );

    expect(getByTestId('icon')).toBeInTheDocument();
    expect(getByTestId('icon')).toHaveAccessibleName('KMS Keyset');
    expect(getByTestId('icon')).toHaveClass('lucide-folder-key');
  });

  test('should render <AuditEntityTypeIcon/> component with a label for profile entity type', () => {
    const { getByTestId } = renderWithMessageProvider(
      <AuditEntityTypeIcon value="profile" data-testid="icon"/>,
    );

    expect(getByTestId('icon')).toBeInTheDocument();
    expect(getByTestId('icon')).toHaveAccessibleName('Service profile');
    expect(getByTestId('icon')).toHaveClass('lucide-git-branch');
  });

  test('should render <AuditEntityTypeIcon/> component with a label for unknown entity type', () => {
    const { getByTestId } = renderWithMessageProvider(
      <AuditEntityTypeIcon value="unknown" data-testid="icon"/>,
    );

    expect(getByTestId('icon')).toBeInTheDocument();
    expect(getByTestId('icon')).toHaveAccessibleName('unknown');
    expect(getByTestId('icon')).toHaveClass('lucide-grid2x2-x');
  });

});
