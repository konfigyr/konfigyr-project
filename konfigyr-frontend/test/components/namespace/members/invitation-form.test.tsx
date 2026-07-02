import { afterAll, beforeAll, describe, expect, test } from 'vitest';
import { InviteForm } from '@konfigyr/components/namespace/members/invitation-form';
import { Toaster } from '@konfigyr/components/ui/sonner';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { namespaces } from '@konfigyr/test/helpers/mocks';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';

import type { RenderResult } from '@testing-library/react';
import type { UserEvent } from '@testing-library/user-event';

describe('components | namespace | members | <InviteForm/>', () => {
  let user: UserEvent;
  let result: RenderResult;

  beforeAll(() => {
    user = userEvents.setup();
    result = renderWithQueryClient((
      <>
        <InviteForm namespace={namespaces.konfigyr} />
        <Toaster />
      </>
    ));
  });

  afterAll(() => cleanup());

  test('should render namespace invitation form', async () => {
    await waitFor(() => {
      expect(result.getByRole('textbox')).toBeInTheDocument();
      expect(result.getByRole('textbox')).toHaveAccessibleName('Invite members by email address');
      expect(result.getByRole('switch')).toHaveAccessibleName('User');
      expect(result.getByRole('button', { name: 'Invite' })).toBeInTheDocument();
    });
  });

  test('should render an error notification when invitation can not be sent', async () => {
    await user.type(
      result.getByRole('textbox'),
      'invalid email address',
    );

    await user.click(
      result.getByRole('button'),
    );

    await waitFor(() => {
      expect(result.getByText('Could not send invitation.')).toBeInTheDocument();
    });

    expect(result.getByRole('textbox')).toBeValid();
    expect(result.getByRole('button')).not.toBeDisabled();
  });

  test('should render success notification when invitation is sent', async () => {
    await user.clear(
      result.getByRole('textbox'),
    );

    await user.type(
      result.getByRole('textbox'),
      'invitee@konfigyr.com',
    );

    await user.click(
      result.getByRole('switch'),
    );

    await user.click(
      result.getByRole('button'),
    );

    await waitFor(() => {
      expect(result.getByRole('textbox')).toHaveValue('');
      expect(result.getByRole('switch', { checked: false }));
    });
  });
});
