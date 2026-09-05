import { afterEach, describe, expect, test } from 'vitest';
import { cleanup } from '@testing-library/react';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import { AuditEntityTypeIcon } from '@konfigyr/components/audit/audit-entity-type';

describe('components | audit | <AuditEntityTypeIcon/>', () => {
  afterEach(() => cleanup());

  test('should render <AuditEntityTypeIcon/> component with a label for account entity type', () => {
    const { getByTestId } = renderWithMessageProvider(
      <AuditEntityTypeIcon value="account" data-testid="icon"/>,
    );

    expect(getByTestId('icon')).toBeInTheDocument();
    expect(getByTestId('icon')).toHaveAccessibleName('Account');
    expect(getByTestId('icon')).toHaveClass('lucide-circle-user');
  });

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

  test('should render <AuditEntityTypeIcon/> component with a label for namespace trusted issuer entity type', () => {
    const { getByTestId } = renderWithMessageProvider(
      <AuditEntityTypeIcon value="namespace-trusted-issuer" data-testid="icon"/>,
    );

    expect(getByTestId('icon')).toBeInTheDocument();
    expect(getByTestId('icon')).toHaveAccessibleName('Trusted issuer');
    expect(getByTestId('icon')).toHaveClass('lucide-shield-check');
  });

  test('should render <AuditEntityTypeIcon/> component with a label for invitation entity type', () => {
    const { getByTestId } = renderWithMessageProvider(
      <AuditEntityTypeIcon value="invitation" data-testid="icon"/>,
    );

    expect(getByTestId('icon')).toBeInTheDocument();
    expect(getByTestId('icon')).toHaveAccessibleName('Invitation');
    expect(getByTestId('icon')).toHaveClass('lucide-mail-plus');
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

  test('should render <AuditEntityTypeIcon/> component with a label for artifact version entity type', () => {
    const { getByTestId } = renderWithMessageProvider(
      <AuditEntityTypeIcon value="artifact-version" data-testid="icon"/>,
    );

    expect(getByTestId('icon')).toBeInTheDocument();
    expect(getByTestId('icon')).toHaveAccessibleName('Artifact version');
    expect(getByTestId('icon')).toHaveClass('lucide-tag');
  });

  test('should render <AuditEntityTypeIcon/> component with a label for artifact ownership transfer entity type', () => {
    const { getByTestId } = renderWithMessageProvider(
      <AuditEntityTypeIcon value="artifact-ownership-transfer" data-testid="icon"/>,
    );

    expect(getByTestId('icon')).toBeInTheDocument();
    expect(getByTestId('icon')).toHaveAccessibleName('Ownership transfer');
    expect(getByTestId('icon')).toHaveClass('lucide-arrow-left-right');
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
