import { afterEach, describe, expect, test } from 'vitest';
import { cleanup } from '@testing-library/react';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import {
  ConflictingOwnersAlert,
  GroupVerificationState,
  GroupVerificationStateAlert,
  GroupVerificationStateBadge,
} from '@konfigyr/components/groups/group-verification-state';

describe('components | groups | <GroupVerificationState/>', () => {
  afterEach(() => cleanup());

  test('should render a label for an active verification', () => {
    const { getByText } = renderWithMessageProvider(
      <GroupVerificationState state="ACTIVE" />,
    );

    expect(getByText('Active')).toBeInTheDocument();
  });

  test('should render a label for a pending verification', () => {
    const { getByText } = renderWithMessageProvider(
      <GroupVerificationState state="PENDING" />,
    );

    expect(getByText('Pending')).toBeInTheDocument();
  });

  test('should render a label for a failed verification', () => {
    const { getByText } = renderWithMessageProvider(
      <GroupVerificationState state="FAILED" />,
    );

    expect(getByText('Failed')).toBeInTheDocument();
  });

  test('should render a label for a revoked verification', () => {
    const { getByText } = renderWithMessageProvider(
      <GroupVerificationState state="REVOKED" />,
    );

    expect(getByText('Revoked')).toBeInTheDocument();
  });

  test('should render nothing for an unknown state', () => {
    const { container } = renderWithMessageProvider(
      <GroupVerificationState state="UNKNOWN" />,
    );

    expect(container).toBeEmptyDOMElement();
  });
});

describe('components | groups | <GroupVerificationStateBadge/>', () => {
  afterEach(() => cleanup());

  test('should render an active verification badge', () => {
    const { getByText } = renderWithMessageProvider(
      <GroupVerificationStateBadge state="ACTIVE" data-testid="badge" />,
    );

    expect(getByText('Active')).toBeInTheDocument();
    expect(getByText('Active').closest('[data-testid="badge"]')).toHaveClass('bg-success/10');
  });

  test('should render a pending verification badge', () => {
    const { getByText } = renderWithMessageProvider(
      <GroupVerificationStateBadge state="PENDING" data-testid="badge" />,
    );

    expect(getByText('Pending')).toBeInTheDocument();
    expect(getByText('Pending').closest('[data-testid="badge"]')).toHaveClass('bg-warning/10');
  });

  test('should render a failed verification badge', () => {
    const { getByText } = renderWithMessageProvider(
      <GroupVerificationStateBadge state="FAILED" data-testid="badge" />,
    );

    expect(getByText('Failed')).toBeInTheDocument();
    expect(getByText('Failed').closest('[data-testid="badge"]')).toHaveClass('bg-destructive/10');
  });

  test('should render a revoked verification badge', () => {
    const { getByText } = renderWithMessageProvider(
      <GroupVerificationStateBadge state="REVOKED" data-testid="badge" />,
    );

    expect(getByText('Revoked')).toBeInTheDocument();
    expect(getByText('Revoked').closest('[data-testid="badge"]')).toHaveClass('bg-secondary');
  });

  test('should render nothing for an unknown state', () => {
    const { queryByTestId } = renderWithMessageProvider(
      <GroupVerificationStateBadge state="UNKNOWN" data-testid="badge" />,
    );

    expect(queryByTestId('badge')).not.toBeInTheDocument();
  });
});

describe('components | groups | <GroupVerificationStateAlert/>', () => {
  afterEach(() => cleanup());

  test('should not render an alert for an active verification', () => {
    const { getByRole } = renderWithMessageProvider(
      <GroupVerificationStateAlert groupId="com.example.group" verificationState="ACTIVE" />,
    );

    expect(getByRole('alert')).toBeInTheDocument();
    expect(getByRole('alert')).toHaveAccessibleName('Ownership verified');
    expect(getByRole('alert')).toHaveAccessibleDescription(
      'This namespace can publish artifact metadata under com.example.group and its sub-groups.',
    );
  });

  test('should render a pending source-code verification alert', () => {
    const { getByRole } = renderWithMessageProvider(
      <GroupVerificationStateAlert
        groupId="com.github.acme"
        verificationChallenge={{ method: 'SOURCE_CODE', token: 'token' }}
        verificationState="PENDING"
      />,
    );

    expect(getByRole('alert')).toBeInTheDocument();
    expect(getByRole('alert')).toHaveAccessibleName('Claim pending');
    expect(getByRole('alert')).toHaveAccessibleDescription(
      'Create the marker repository for com.github.acme on GitHub, then re-check this claim.',
    );
  });

  test('should render a pending dns verification alert', () => {
    const { getByRole } = renderWithMessageProvider(
      <GroupVerificationStateAlert
        groupId="com.example.group"
        verificationChallenge={{ method: 'DNS', token: 'token' }}
        verificationState="PENDING"
      />,
    );

    expect(getByRole('alert')).toBeInTheDocument();
    expect(getByRole('alert')).toHaveAccessibleName('Claim pending');
    expect(getByRole('alert')).toHaveAccessibleDescription(
      'Publishing under com.example.group is blocked until the TXT record is visible in DNS.',
    );
  });

  test('should render a failed verification alert', () => {
    const { getByRole } = renderWithMessageProvider(
      <GroupVerificationStateAlert groupId="com.example.group" verificationState="FAILED" />,
    );

    expect(getByRole('alert')).toBeInTheDocument();
    expect(getByRole('alert')).toHaveAccessibleName('Verification failed');
    expect(getByRole('alert')).toHaveAccessibleDescription(
      'The claim could not be verified. Fix the record or repository and try again.',
    );
  });

  test('should render a revoked verification alert by default', () => {
    const { getByRole } = renderWithMessageProvider(
      <GroupVerificationStateAlert groupId="com.example.group" verificationState="REVOKED" />,
    );

    expect(getByRole('alert')).toBeInTheDocument();
    expect(getByRole('alert')).toHaveAccessibleName('Claim revoked');
    expect(getByRole('alert')).toHaveAccessibleDescription(
      'Publishing under com.example.group is blocked. You can claim this groupId again at any time.',
    );
  });
});

describe('components | groups | <ConflictingOwnersAlert/>', () => {
  afterEach(() => cleanup());

  test('should render a warning banner listing a single conflicting owner', () => {
    const { getByRole } = renderWithMessageProvider(
      <ConflictingOwnersAlert conflictingOwners={['ebf']} />,
    );

    expect(getByRole('alert')).toBeInTheDocument();
    expect(getByRole('alert')).toHaveAccessibleName('Other namespaces already own artifacts here');
    expect(getByRole('alert')).toHaveAccessibleDescription(
      'Namespace ebf already publishes artifacts under this groupId. You may need to request an ownership transfer before you can publish.',
    );
  });

  test('should render a warning banner listing multiple conflicting owners', () => {
    const { getByRole } = renderWithMessageProvider(
      <ConflictingOwnersAlert conflictingOwners={['ebf', 'acme-legacy']} />,
    );

    expect(getByRole('alert')).toHaveAccessibleDescription(
      'Namespaces ebf, acme-legacy already publish artifacts under this groupId. You may need to request an ownership transfer before you can publish.',
    );
  });
});
