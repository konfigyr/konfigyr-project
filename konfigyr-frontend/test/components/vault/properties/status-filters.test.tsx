import { afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import { PropertyStatusFilters } from '@konfigyr/components/vault/properties/status-filters';
import { profiles } from '@konfigyr/test/helpers/mocks';

const changeset = {
  profile: profiles.development,
  name: 'test changeset',
  state: 'DRAFT',
  properties: [],
  added: 1,
  modified: 2,
  deleted: 3,
};

describe('components | vault | properties | <StatusFilters/>', () => {
  const onChange = vi.fn();

  afterEach(() => {
    cleanup();
    vi.resetAllMocks();
  });

  test('should render property filter state for changeset with accessible elements', () => {
    const result = renderWithMessageProvider(
      <PropertyStatusFilters
        changeset={changeset}
        value="all"
        onChange={onChange}
      />,
    );

    expect(result.getByRole('radiogroup', { name: 'Property status filters' })).toBeInTheDocument();
    expect(result.getByRole('radio', { name: 'All' })).toBeInTheDocument();
    expect(result.getByRole('radio', { name: 'Added' })).toBeInTheDocument();
    expect(result.getByRole('radio', { name: 'Modified' })).toBeInTheDocument();
    expect(result.getByRole('radio', { name: 'Deleted' })).toBeInTheDocument();

    expect(result.getByRole('radio', { name: 'All' })).toBeChecked();
  });

  test('should invoke on change handler when option is clicked', async () => {
    const result = renderWithMessageProvider(
      <PropertyStatusFilters
        changeset={changeset}
        value="deleted"
        onChange={onChange}
      />,
    );

    expect(result.getByRole('radio', { name: 'All' })).not.toBeChecked();
    expect(result.getByRole('radio', { name: 'Deleted' })).toBeChecked();

    // assert on change handler
    await userEvents.click(result.getByRole('radio', { name: 'Modified' }));

    await waitFor(() => {
      expect(onChange).toHaveBeenCalledExactlyOnceWith('modified');
    });
  });
});
