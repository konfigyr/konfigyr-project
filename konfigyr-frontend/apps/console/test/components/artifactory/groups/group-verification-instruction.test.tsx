import { afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import {
  DnsActivationInstruction,
  SourceCodeActivationInstruction,
} from '@konfigyr/components/artifactory/groups/group-verification-instruction';

import type { VerificationChallenge } from '@konfigyr/hooks/types';

const clipboard = vi.hoisted(() => vi.fn().mockResolvedValue(true));

vi.mock('@konfigyr/hooks/clipboard', () => ({
  useClipboard: () => clipboard,
}));

describe('components | groups | <DnsActivationInstruction/>', () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  test('should render the dns activation checklist and copy token', async () => {
    const user = userEvents.setup();

    const { getByRole, getByText } = renderWithMessageProvider(
      <DnsActivationInstruction
        groupId="com.example"
        challenge={{
          id: 'challenge-1',
          verificationId: 'verification-1',
          method: 'DNS',
          token: 'token-123',
          state: 'UNVERIFIED',
          createdAt: '2026-07-07T00:00:00Z',
          verifiedAt: null,
          expiresAt: null,
        } satisfies VerificationChallenge}
      />,
    );

    expect(getByText('Add a DNS TXT record')).toBeInTheDocument();
    expect(getByText('Follow the steps below to prove ownership of com.example.')).toBeInTheDocument();
    expect(getByText('Sign in to wherever you manage DNS for example.com.')).toBeInTheDocument();
    expect(getByText('konfigyr-verification=token-123')).toBeInTheDocument();
    expect(getByText('Save the record')).toBeInTheDocument();
    expect(getByText('Propagation can take up to 48 hours. You do not need to wait on this page.')).toBeInTheDocument();

    await user.click(getByRole('button', { name: 'Copy' }));

    await waitFor(() => {
      expect(clipboard).toHaveBeenCalledExactlyOnceWith('konfigyr-verification=token-123');
      expect(getByRole('button')).toHaveTextContent('Copied');
    });
  });
});

describe('components | groups | <SourceCodeActivationInstruction/>', () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  test('should render the source code activation checklist and copy token', async () => {
    const user = userEvents.setup();

    const { getByRole, getByText } = renderWithMessageProvider(
      <SourceCodeActivationInstruction
        groupId="com.github.acme"
        challenge={{
          id: 'challenge-2',
          verificationId: 'verification-2',
          method: 'SOURCE_CODE',
          token: 'token-456',
          state: 'UNVERIFIED',
          createdAt: '2026-07-07T00:00:00Z',
          verifiedAt: null,
          expiresAt: null,
        } satisfies VerificationChallenge}
      />,
    );

    expect(getByText('Create a public repository on GitHub')).toBeInTheDocument();
    expect(getByText('Follow the steps below to prove ownership of the acme account on GitHub.')).toBeInTheDocument();
    expect(getByText('Open GitHub')).toBeInTheDocument();
    expect(getByText('Sign in to GitHub with account acme.')).toBeInTheDocument();
    expect(getByText('KFGYR-token-456')).toBeInTheDocument();
    expect(getByText('Save the repository')).toBeInTheDocument();
    expect(getByText('Return here and click Verify. We will check that the public repository exists in GitHub account.')).toBeInTheDocument();

    await user.click(getByRole('button', { name: 'Copy' }));

    await waitFor(() => {
      expect(clipboard).toHaveBeenCalledExactlyOnceWith('KFGYR-token-456');
      expect(getByRole('button')).toHaveTextContent('Copied');
    });
  });
});
