import { afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { startOfDay, subDays } from 'date-fns';
import { enUS } from 'date-fns/locale/en-US';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import { AuditRecordFilters } from '@konfigyr/components/audit/audit-record-filters';

const createCalendarDate = (delta: number) => {
  const date = subDays(new Date(), delta);
  return startOfDay(date);
};

const formatCalendarDateSelector = (date: Date) => {
  const formatted = date.toLocaleDateString(enUS.code);
  return `button[data-day='${formatted}']`;
};

describe('components | audit | <AuditRecordFilters/>', () => {
  afterEach(() => {
    vi.resetAllMocks();
    cleanup();
  });

  test('should render <AuditRecordFilters/> component with empty filter values', () => {
    const { getByRole } = renderWithMessageProvider(
      <AuditRecordFilters query={{}} onQueryChange={vi.fn()} />,
    );

    expect(getByRole('combobox', { name: 'Filter by entity type' })).toBeInTheDocument();
    expect(getByRole('combobox', { name: 'Filter by entity type' })).toHaveTextContent('Filter by entity type');

    expect(getByRole('button', { name: 'Select date range' })).toBeInTheDocument();
  });

  test('should render <AuditRecordFilters/> component with a selected entity type', () => {
    const { getByRole } = renderWithMessageProvider(
      <AuditRecordFilters query={{ entityType: 'namespace' }} onQueryChange={vi.fn()} />,
    );

    expect(getByRole('combobox', { name: 'Filter by entity type' })).toBeInTheDocument();
    expect(getByRole('combobox', { name: 'Filter by entity type' })).toHaveTextContent('Namespace');
  });

  test('should render <AuditRecordFilters/> component with only from date range selected', () => {
    const { getByRole } = renderWithMessageProvider(
      <AuditRecordFilters query={{ from: '2026-04-29' }} onQueryChange={vi.fn()} />,
    );

    expect(getByRole('button', { name: 'Apr 29, 2026' })).toBeInTheDocument();
  });

  test('should render <AuditRecordFilters/> component with a date range selected', () => {
    const { getByRole } = renderWithMessageProvider(
      <AuditRecordFilters query={{ from: '2026-04-27', to: '2026-04-29' }} onQueryChange={vi.fn()} />,
    );

    expect(getByRole('button', { name: 'Apr 27, 2026 - Apr 29, 2026' })).toBeInTheDocument();
  });

  test('should select entity type from the combobox', async () => {
    const onChange = vi.fn();

    const { getAllByRole, getByRole } = renderWithMessageProvider(
      <AuditRecordFilters query={{ entityType: 'namespace', size: 20 }} onQueryChange={onChange} />,
    );

    await userEvents.click(
      getByRole('combobox', { name: 'Filter by entity type' }),
    );

    await waitFor(() => {
      expect(getByRole('listbox')).toBeInTheDocument();
    });

    expect(getAllByRole('option')).toHaveLength(5);
    expect(getByRole('option', { name: 'Namespace' })).toBeInTheDocument();
    expect(getByRole('option', { name: 'Application' })).toBeInTheDocument();
    expect(getByRole('option', { name: 'KMS Keyset' })).toBeInTheDocument();
    expect(getByRole('option', { name: 'Service' })).toBeInTheDocument();
    expect(getByRole('option', { name: 'Service profile' })).toBeInTheDocument();

    await userEvents.click(
      getByRole('option', { name: 'Service profile' }),
    );

    await waitFor(() => {
      expect(onChange).toHaveBeenCalledExactlyOnceWith({ entityType: 'profile', size: 20 });
    });
  });

  test('should select date range from the calendar filter', async () => {
    const onChange = vi.fn();

    const { baseElement, getByRole } = renderWithMessageProvider(
      <AuditRecordFilters query={{ size: 20 }} onQueryChange={onChange} />,
    );

    await userEvents.click(
      getByRole('button', { name: 'Select date range' }),
    );

    await waitFor(() => {
      expect(getByRole('dialog')).toBeInTheDocument();
    });

    const fromDate = createCalendarDate(5);
    const toDate = createCalendarDate(2);

    const fromButton = baseElement.querySelector(formatCalendarDateSelector(fromDate));
    expect(fromButton).toBeInTheDocument();
    await userEvents.click(fromButton!);

    const toButton = baseElement.querySelector(formatCalendarDateSelector(toDate));
    expect(toButton).toBeInTheDocument();
    await userEvents.click(toButton!);

    await waitFor(() => {
      expect(onChange).toHaveBeenCalledExactlyOnceWith({
        size: 20,
        from: fromDate.toISOString(),
        to: toDate.toISOString(),
      });
    });
  });
});
