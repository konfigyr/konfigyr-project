import { afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
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
      <AuditRecordFilters debounceMs={0} query={{}} onQueryChange={vi.fn()} />,
    );

    expect(getByRole('combobox', { name: 'Filter by entity type' })).toBeInTheDocument();
    expect(getByRole('combobox', { name: 'Filter by entity type' })).toHaveTextContent('Filter by entity type');

    expect(getByRole('button', { name: 'Select date range' })).toBeInTheDocument();
  });

  test('should render <AuditRecordFilters/> component with a selected entity type', () => {
    const { getByRole } = renderWithMessageProvider(
      <AuditRecordFilters debounceMs={0} query={{ entityType: 'namespace' }} onQueryChange={vi.fn()} />,
    );

    expect(getByRole('combobox', { name: 'Filter by entity type' })).toBeInTheDocument();
    expect(getByRole('combobox', { name: 'Filter by entity type' })).toHaveTextContent('Namespace');
  });

  test('should render <AuditRecordFilters/> component with only from date range selected', () => {
    const { getByRole } = renderWithMessageProvider(
      <AuditRecordFilters debounceMs={0} query={{ from: '2026-04-29' }} onQueryChange={vi.fn()} />,
    );

    expect(getByRole('button', { name: 'Apr 29, 2026' })).toBeInTheDocument();
  });

  test('should render <AuditRecordFilters/> component with a date range selected', () => {
    const { getByRole } = renderWithMessageProvider(
      <AuditRecordFilters debounceMs={0} query={{ from: '2026-04-27', to: '2026-04-29' }} onQueryChange={vi.fn()} />,
    );

    expect(getByRole('button', { name: 'Apr 27, 2026 - Apr 29, 2026' })).toBeInTheDocument();
  });

  test('should select entity type from the combobox', async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();

    const { getByRole, findByRole } = renderWithMessageProvider(
      <AuditRecordFilters debounceMs={0} query={{ entityType: 'namespace', size: 20 }} onQueryChange={onChange} />,
    );

    await user.click(getByRole('combobox', { name: 'Filter by entity type' }));

    const listbox = await findByRole('listbox');
    const options = within(listbox).getAllByRole('option');

    expect(options).toHaveLength(9);
    expect(within(listbox).getByRole('option', { name: 'Namespace' })).toBeInTheDocument();
    expect(within(listbox).getByRole('option', { name: 'Application' })).toBeInTheDocument();
    expect(within(listbox).getByRole('option', { name: 'Trusted issuer' })).toBeInTheDocument();
    expect(within(listbox).getByRole('option', { name: 'Invitation' })).toBeInTheDocument();
    expect(within(listbox).getByRole('option', { name: 'Artifact version' })).toBeInTheDocument();
    expect(within(listbox).getByRole('option', { name: 'Ownership transfer' })).toBeInTheDocument();
    expect(within(listbox).getByRole('option', { name: 'KMS Keyset' })).toBeInTheDocument();
    expect(within(listbox).getByRole('option', { name: 'Service' })).toBeInTheDocument();
    expect(within(listbox).getByRole('option', { name: 'Service profile' })).toBeInTheDocument();

    await user.click(within(listbox).getByRole('option', { name: 'Service profile' }));

    expect(onChange).toHaveBeenCalledExactlyOnceWith({ entityType: 'profile', size: 20 });
  });

  test('should select date range from the calendar filter', async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();

    const { baseElement, getByRole } = renderWithMessageProvider(
      <AuditRecordFilters debounceMs={0} query={{ size: 20 }} onQueryChange={onChange} />,
    );

    await user.click(getByRole('button', { name: 'Select date range' }));

    expect(getByRole('dialog')).toBeInTheDocument();

    const fromDate = createCalendarDate(5);
    const toDate = createCalendarDate(2);

    const fromButton = baseElement.querySelector(formatCalendarDateSelector(fromDate));
    expect(fromButton).toBeInTheDocument();
    await user.click(fromButton!);

    const toButton = baseElement.querySelector(formatCalendarDateSelector(toDate));
    expect(toButton).toBeInTheDocument();
    await user.click(toButton!);

    expect(onChange).toHaveBeenCalledExactlyOnceWith({
      size: 20,
      from: fromDate.toISOString(),
      to: toDate.toISOString(),
    });
  });
});
