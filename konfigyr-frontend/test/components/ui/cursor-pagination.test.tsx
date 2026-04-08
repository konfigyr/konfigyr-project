import { afterEach, describe, expect, test, vi } from 'vitest';
import { CursorPagination } from '@konfigyr/components/ui/pagination';
import { cleanup, fireEvent } from '@testing-library/react';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';

import type { CursorPaginationProps } from '@konfigyr/components/ui/pagination';

describe('components | UI | <CursorPagination/>', () => {
  const onChange: CursorPaginationProps['onChange'] = vi.fn();

  afterEach(() => {
    vi.resetAllMocks();
    cleanup();
  });

  test('should render pagination with both links disabled', () => {
    const { getAllByRole, getByRole } = renderWithMessageProvider(
      <CursorPagination onChange={onChange} />,
    );

    expect(getByRole('navigation', { name: 'pagination' })).toBeInTheDocument();

    const items = getAllByRole('listitem');
    expect(items).toHaveLength(2);

    expect(items[0].firstChild).toHaveAccessibleName('Go to previous page');
    expect(items[0].firstChild).toHaveAttribute('aria-disabled', 'true');

    expect(items[1].firstChild).toHaveAccessibleName('Go to next page');
    expect(items[1].firstChild).toHaveAttribute('aria-disabled', 'true');

    fireEvent.click(items[0]);
    fireEvent.click(items[1]);

    expect(onChange).not.toHaveBeenCalled();
  });

  test('should render pagination with previous link disabled', () => {
    const { getByRole } = renderWithMessageProvider(
      <CursorPagination next="next-token" onChange={onChange} />,
    );

    expect(getByRole('button', { name: 'Go to previous page' }))
      .toHaveAttribute('aria-disabled', 'true');

    expect(getByRole('button', { name: 'Go to next page' }))
      .toHaveAttribute('aria-disabled', 'false');

    fireEvent.click(getByRole('button', { name: 'Go to next page' }));

    expect(onChange).toHaveBeenCalledExactlyOnceWith('next-token', 10);
  });

  test('should render pagination with next link disabled', () => {
    const { getByRole } = renderWithMessageProvider(
      <CursorPagination previous="previous-token" size={44} onChange={onChange} />,
    );

    expect(getByRole('button', { name: 'Go to previous page' }))
      .toHaveAttribute('aria-disabled', 'false');

    expect(getByRole('button', { name: 'Go to next page' }))
      .toHaveAttribute('aria-disabled', 'true');

    fireEvent.click(getByRole('button', { name: 'Go to previous page' }));

    expect(onChange).toHaveBeenCalledExactlyOnceWith('previous-token', 44);
  });
});
