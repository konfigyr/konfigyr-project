import { afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { ChangeRequestState } from '@konfigyr/hooks/vault/types';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { ChangeRequestFilters } from '@konfigyr/components/vault/change-request/change-request-filters';
import { namespaces, services } from '@konfigyr/test/helpers/mocks';

describe('components | vault | change-request | <ChangeRequestFilters/>', () => {
  afterEach(() => {
    cleanup();
    vi.resetAllMocks();
  });

  afterEach(() => cleanup());

  test('should render <ChangeRequestFilters/> component', () => {
    const { getByRole } = renderWithQueryClient(
      <ChangeRequestFilters
        namespace={namespaces.konfigyr}
        service={services.konfigyrApi}
        query={{}}
        onQueryChange={vi.fn()}
      />,
    );

    expect(getByRole('form')).toBeInTheDocument();
    expect(getByRole('searchbox', { name: 'Search change requests' })).toBeInTheDocument();
    expect(getByRole('combobox', { name: 'Filter by state' })).toBeInTheDocument();
    expect(getByRole('combobox', { name: 'Filter by profile' })).toBeInTheDocument();
    expect(getByRole('combobox', { name: 'Sort by' })).toBeInTheDocument();
  });

  test('should search by search term', async () => {
    const onQueryChange = vi.fn();
    const { getByRole } = renderWithQueryClient(
      <ChangeRequestFilters
        namespace={namespaces.konfigyr}
        service={services.konfigyrApi}
        query={{}}
        onQueryChange={onQueryChange}
      />,
    );

    await userEvents.type(
      getByRole('searchbox'),
      'search term',
    );

    await waitFor(() => {
      expect(onQueryChange).toHaveBeenCalledExactlyOnceWith({
        term: 'search term',
        profile: undefined,
        sort: 'date,desc',
        state: ChangeRequestState.OPEN,
      });
    });
  });

  test('should sort by last modified', async () => {
    const onQueryChange = vi.fn();
    const { getByRole } = renderWithQueryClient(
      <ChangeRequestFilters
        namespace={namespaces.konfigyr}
        service={services.konfigyrApi}
        query={{}}
        onQueryChange={onQueryChange}
      />,
    );

    await userEvents.click(
      getByRole('combobox', { name: 'Sort by' }),
    );

    await waitFor(() => {
      expect(getByRole('option', { name: 'Least recently updated' })).toBeInTheDocument();
    });

    await userEvents.click(
      getByRole('option', { name: 'Least recently updated' }),
    );

    await waitFor(() => {
      expect(onQueryChange).toHaveBeenCalledExactlyOnceWith({
        profile: undefined,
        term: undefined,
        sort: 'updated',
        state: ChangeRequestState.OPEN,
      });
    });
  });

  test('should filter by state', async () => {
    const onQueryChange = vi.fn();
    const { getByRole } = renderWithQueryClient(
      <ChangeRequestFilters
        namespace={namespaces.konfigyr}
        service={services.konfigyrApi}
        query={{}}
        onQueryChange={onQueryChange}
      />,
    );

    await userEvents.click(
      getByRole('combobox', { name: 'Filter by state' }),
    );

    await waitFor(() => {
      expect(getByRole('option', { name: 'Merged' })).toBeInTheDocument();
    });

    await userEvents.click(
      getByRole('option', { name: 'Merged' }),
    );

    await waitFor(() => {
      expect(onQueryChange).toHaveBeenCalledExactlyOnceWith({
        profile: undefined,
        term: undefined,
        sort: 'date,desc',
        state: ChangeRequestState.MERGED,
      });
    });
  });

  test('should filter by specific profile', async () => {
    const onQueryChange = vi.fn();
    const { getByRole } = renderWithQueryClient(
      <ChangeRequestFilters
        namespace={namespaces.konfigyr}
        service={services.konfigyrApi}
        query={{}}
        onQueryChange={onQueryChange}
      />,
    );

    await userEvents.click(
      getByRole('combobox', { name: 'Filter by profile' }),
    );

    await waitFor(() => {
      expect(getByRole('option', { name: 'Staging' })).toBeInTheDocument();
    });

    await userEvents.click(
      getByRole('option', { name: 'Staging' }),
    );

    await waitFor(() => {
      expect(onQueryChange).toHaveBeenCalledExactlyOnceWith({
        profile: 'staging',
        term: undefined,
        sort: 'date,desc',
        state: ChangeRequestState.OPEN,
      });
    });
  });

});
