import { afterAll, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { KeysetDetails } from '@konfigyr/components/kms/keyset-details';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { kms, namespaces } from '@konfigyr/test/helpers/mocks';

describe('components | kms | <KeysetDetails/>', () => {
  const user = userEvents.setup();

  const result = renderWithQueryClient(
    <KeysetDetails
      namespace={namespaces.konfigyr}
      keyset={kms.destroyedKeyset}
      keys={kms.destroyedKeyset.keys}
    />,
  );

  afterAll(() => cleanup());

  test('should render keyset details', () => {
    expect(result.getByText(kms.destroyedKeyset.name)).toBeInTheDocument();
    expect(result.getByText(kms.destroyedKeyset.description ?? '')).toBeInTheDocument();

    expect(result.getByText('30 days')).toBeInTheDocument();
    expect(result.getByText('None, keys are deleted immediately')).toBeInTheDocument();
  });

  test('should render key table', () => {
    const table = result.getByRole('table');

    expect(table).toBeInTheDocument();
    expect(result.getByRole('columnheader', { name: 'Key ID' })).toBeInTheDocument();
    expect(result.getByRole('columnheader', { name: 'Algorithm' })).toBeInTheDocument();
    expect(result.getByRole('columnheader', { name: 'State' })).toBeInTheDocument();
    expect(result.getByRole('columnheader', { name: 'Created' })).toBeInTheDocument();
    expect(result.getByRole('columnheader', { name: 'Expiry' })).toBeInTheDocument();
    expect(result.getByRole('columnheader', { name: 'Destruction' })).toBeInTheDocument();

    expect(table.querySelectorAll('tbody tr'))
      .toHaveLength(kms.destroyedKeyset.keys.length);
  });

  test('should open the dropdown menu for a key', async () => {
    await user.click(
      result.getByRole('button', { name: `Open actions menu for key ${kms.destroyedKeyset.keys[0].id}` }),
    );

    await waitFor(() => {
      result.getByRole('menu');
    });

    expect(result.getByRole('menuitem', { name: 'Disable key' })).toBeInTheDocument();
    expect(result.getByRole('menuitem', { name: 'Reactivate key' })).toBeInTheDocument();
    expect(result.getByRole('menuitem', { name: 'Restore key' })).toBeInTheDocument();
    expect(result.getByRole('menuitem', { name: 'Mark as compromised' })).toBeInTheDocument();
    expect(result.getByRole('menuitem', { name: 'Schedule for destruction' })).toBeInTheDocument();
  });

  test('should restore the key that is scheduled for destruction', async () => {
    await user.click(
      result.getByRole('menuitem', { name: 'Restore key' }),
    );

    await waitFor(() => {
      result.getByRole('dialog', { name: 'Restore key?' });
    });

    await user.click(
      result.getByRole('button', { name: 'Yes, I am sure' }),
    );

    await user.keyboard('{Escape}');

    await waitFor(() => {
      expect(result.queryByRole('menu')).not.toBeInTheDocument();
      expect(result.queryByRole('dialog')).not.toBeInTheDocument();
    });
  });
});
