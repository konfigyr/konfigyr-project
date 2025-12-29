import { afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { KeysetFilters } from '@konfigyr/components/kms/keyset-filters';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';

const DEFAULT_QUERY = { term: undefined, algorithm: undefined, state: undefined, sort: undefined };

describe('components | kms | <KeysetFilters/>', () => {
  afterEach(() => cleanup());

  test('should render keyset filters with default values', () => {
    const result = renderWithMessageProvider((
      <KeysetFilters query={DEFAULT_QUERY} onQueryChange={vi.fn()} />
    ));

    expect(result.getByRole('searchbox', { name: 'Search keysets' })).toBeInTheDocument();
    expect(result.getByRole('searchbox', { name: 'Search keysets' })).toHaveValue('');

    expect(result.getByText('Filter by state')).toBeInTheDocument();
    expect(result.getByText('Filter by algorithm')).toBeInTheDocument();
    expect(result.queryByText('Sort by')).not.toBeInTheDocument();
  });

  test('should render keyset filters with custom search term', async () => {
    const query = { ...DEFAULT_QUERY, term: 'search term' };
    const onQueryChange = vi.fn();

    const result = renderWithMessageProvider((
      <KeysetFilters query={query} onQueryChange={onQueryChange} />
    ));

    const input = result.getByRole('searchbox', { name: 'Search keysets' });

    expect(input).toBeInTheDocument();
    expect(input).toHaveValue('search term');

    await userEvents.clear(input);
    await userEvents.type(input, 'new search term');

    await waitFor(() => {
      expect(onQueryChange).toHaveBeenCalledExactlyOnceWith({
        ...DEFAULT_QUERY,
        term: 'new search term',
        sort: 'date',
      });
    });
  });
});
