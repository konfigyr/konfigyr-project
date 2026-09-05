import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { KeyStatusAlert, KeyStatusBadge } from '@konfigyr/components/kms/key-status';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';

describe('components | kms | <KeyStatusAlert/>', () => {
  afterEach(() => cleanup());

  test('should not render alert for an active key', () => {
    const { queryByRole } = renderWithMessageProvider(
      <KeyStatusAlert status='ENABLED' />,
    );

    expect(queryByRole('alert')).not.toBeInTheDocument();
  });

  test('should render alert for a disabled key', () => {
    const { getByRole } = renderWithMessageProvider(
      <KeyStatusAlert status='DISABLED' />,
    );

    expect(getByRole('alert')).toBeInTheDocument();
    expect(getByRole('alert')).toHaveAccessibleName('This keyset is disabled');
    expect(getByRole('alert')).toHaveAccessibleDescription('No cryptographic operations are allowed. Reactivate it to resume use.');
  });

  test('should render alert for a compromised key', () => {
    const { getByRole } = renderWithMessageProvider(
      <KeyStatusAlert status='COMPROMISED' />,
    );

    expect(getByRole('alert')).toBeInTheDocument();
    expect(getByRole('alert')).toHaveAccessibleName('This keyset is compromised');
    expect(getByRole('alert')).toHaveAccessibleDescription('Rotate immediately and re-protect any data it was used on.');
  });

  test('should render alert for a key scheduled for destruction', () => {
    const { getByRole } = renderWithMessageProvider(
      <KeyStatusAlert status='PENDING_DESTRUCTION' />,
    );

    expect(getByRole('alert')).toBeInTheDocument();
    expect(getByRole('alert')).toHaveAccessibleName('This keyset is pending deletion');
    expect(getByRole('alert')).toHaveAccessibleDescription('No cryptographic operations are allowed. You can still cancel deletion before the grace period ends.');
  });

  test('should render alert for a destroyed key', () => {
    const { getByRole } = renderWithMessageProvider(
      <KeyStatusAlert status='DESTROYED' />,
    );

    expect(getByRole('alert')).toBeInTheDocument();
    expect(getByRole('alert')).toHaveAccessibleName('This keyset has been permanently deleted');
    expect(getByRole('alert')).toHaveAccessibleDescription('The key material cannot be recovered and any data it protected is no longer accessible.');
  });

  test('should render alert for a key that failed to be initialized', () => {
    const { getByRole } = renderWithMessageProvider(
      <KeyStatusAlert status='INITIALIZATION_FAILED' />,
    );

    expect(getByRole('alert')).toBeInTheDocument();
    expect(getByRole('alert')).toHaveAccessibleName('Keyset initialization failed');
    expect(getByRole('alert')).toHaveAccessibleDescription('Key material could not be generated. The keyset is unusable. Delete it and create a new one to retry.');
  });

  test('should render alert for a key that failed to be destroyed', () => {
    const { getByRole } = renderWithMessageProvider(
      <KeyStatusAlert status='DESTRUCTION_FAILED' />,
    );

    expect(getByRole('alert')).toBeInTheDocument();
    expect(getByRole('alert')).toHaveAccessibleName('Deletion failed');
    expect(getByRole('alert')).toHaveAccessibleDescription('An error occurred while trying to destroy this keyset. The key material may still exist. Retry or contact support.');
  });
});

describe('components | kms | <KeyStatusBadge/>', () => {
  afterEach(() => cleanup());

  test('should show a tooltip when hovering over key status badge', async () => {
    const user = userEvents.setup();
    const { getByRole, getByText } = renderWithMessageProvider(
      <KeyStatusBadge status='ENABLED' delay={50} />,
    );

    await user.hover(getByRole('button'));

    await waitFor(() => {
      expect(getByText('This key is active and being used for all new cryptographic operations.'))
        .toBeInTheDocument();
    });
  });

  test('should render status badge for enabled key', () => {
    const { getByRole } = renderWithMessageProvider(
      <KeyStatusBadge status='ENABLED' />,
    );

    expect(getByRole('button', { name: 'Active' })).toBeInTheDocument();
  });

  test('should render status badge for disabled key', () => {
    const { getByRole } = renderWithMessageProvider(
      <KeyStatusBadge status='DISABLED' />,
    );

    expect(getByRole('button', { name: 'Disabled' })).toBeInTheDocument();
  });

  test('should render status badge for compromised key', () => {
    const { getByRole } = renderWithMessageProvider(
      <KeyStatusBadge status='COMPROMISED' />,
    );

    expect(getByRole('button', { name: 'Compromised' })).toBeInTheDocument();
  });

  test('should render status badge for a key scheduled for destruction', () => {
    const { getByRole } = renderWithMessageProvider(
      <KeyStatusBadge status='PENDING_DESTRUCTION' />,
    );

    expect(getByRole('button', { name: 'Pending deletion' })).toBeInTheDocument();
  });

  test('should render status badge for destroyed key', () => {
    const { getByRole } = renderWithMessageProvider(
      <KeyStatusBadge status='DESTROYED' />,
    );

    expect(getByRole('button', { name: 'Destroyed' })).toBeInTheDocument();
  });

  test('should render status badge for a key that is initializing', () => {
    const { getByRole } = renderWithMessageProvider(
      <KeyStatusBadge status='INITIALIZING' />,
    );

    expect(getByRole('button', { name: 'Setting up' })).toBeInTheDocument();
  });

  test('should render status badge for a key that failed to be initialized', () => {
    const { getByRole } = renderWithMessageProvider(
      <KeyStatusBadge status='INITIALIZATION_FAILED' />,
    );

    expect(getByRole('button', { name: 'Setup failed' })).toBeInTheDocument();
  });

  test('should render status badge for a key that failed to be destroyed', () => {
    const { getByRole } = renderWithMessageProvider(
      <KeyStatusBadge status='DESTRUCTION_FAILED' />,
    );

    expect(getByRole('button', { name: 'Deletion failed' })).toBeInTheDocument();
  });
});
