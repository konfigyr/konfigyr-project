import { afterEach, describe, expect, test } from 'vitest';
import {
  Pagination,
  PaginationContent,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
  PaginationRange,
} from '@konfigyr/components/ui/pagination';
import { cleanup } from '@testing-library/react';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';

import type { PaginationProps } from '@konfigyr/components/ui/pagination';

function PaginationComponent({ range, ...props }: PaginationProps & { range?: number }) {
  return (
    <Pagination {...props}>
      <PaginationContent>
        <PaginationItem>
          <PaginationPrevious />
        </PaginationItem>
        <PaginationRange range={range}>
          {state => (
            <PaginationLink isActive={state.active}>{state.page}</PaginationLink>
          )}
        </PaginationRange>
        <PaginationItem>
          <PaginationNext />
        </PaginationItem>
      </PaginationContent>
    </Pagination>
  );
}

describe('components | UI | <Pagination/>', () => {
  afterEach(() => cleanup());

  test('should render pagination component with first page active', () => {
    const { getAllByRole, getByRole } = renderWithMessageProvider(
      <PaginationComponent page={1} pages={12} range={7} />,
    );

    expect(getByRole('navigation', { name: 'pagination' })).toBeInTheDocument();

    const items = getAllByRole('listitem');
    expect(items).toHaveLength(9);

    expect(items[0].firstChild).toHaveAccessibleName('Go to previous page');
    expect(items[0].firstChild).toHaveAttribute('aria-disabled', 'true');

    expect(items[1].firstChild).toHaveAttribute('aria-current', 'page');
    expect(items[1].firstChild).toHaveTextContent('1');
    expect(items[2].firstChild).toHaveTextContent('2');
    expect(items[3].firstChild).toHaveTextContent('3');
    expect(items[4].firstChild).toHaveTextContent('4');
    expect(items[5].firstChild).toHaveTextContent('5');
    expect(items[6].firstChild).toHaveTextContent('6');
    expect(items[7].firstChild).toHaveTextContent('7');

    expect(items[8].firstChild).toHaveAccessibleName('Go to next page');
    expect(items[8].firstChild).toHaveAttribute('aria-disabled', 'false');
  });

  test('should render pagination component with third page active', () => {
    const { getAllByRole, getByRole } = renderWithMessageProvider(
      <PaginationComponent page={3} pages={11} range={4} />,
    );

    expect(getByRole('navigation', { name: 'pagination' })).toBeInTheDocument();

    const items = getAllByRole('listitem');
    expect(items).toHaveLength(6);

    expect(items[0].firstChild).toHaveAccessibleName('Go to previous page');
    expect(items[0].firstChild).toHaveAttribute('aria-disabled', 'false');

    expect(items[1].firstChild).toHaveTextContent('2');
    expect(items[2].firstChild).toHaveTextContent('3');
    expect(items[2].firstChild).toHaveAttribute('aria-current', 'page');
    expect(items[3].firstChild).toHaveTextContent('4');
    expect(items[4].firstChild).toHaveTextContent('5');

    expect(items[5].firstChild).toHaveAccessibleName('Go to next page');
    expect(items[5].firstChild).toHaveAttribute('aria-disabled', 'false');
  });

  test('should render pagination component with last page active', () => {
    const { getAllByRole, getByRole } = renderWithMessageProvider(
      <PaginationComponent page={9} pages={9} range={5} />,
    );

    expect(getByRole('navigation', { name: 'pagination' })).toBeInTheDocument();

    const items = getAllByRole('listitem');
    expect(items).toHaveLength(7);

    expect(items[0].firstChild).toHaveAccessibleName('Go to previous page');
    expect(items[0].firstChild).toHaveAttribute('aria-disabled', 'false');

    expect(items[1].firstChild).toHaveTextContent('5');
    expect(items[2].firstChild).toHaveTextContent('6');
    expect(items[3].firstChild).toHaveTextContent('7');
    expect(items[4].firstChild).toHaveTextContent('8');
    expect(items[5].firstChild).toHaveAttribute('aria-current', 'page');
    expect(items[5].firstChild).toHaveTextContent('9');

    expect(items[6].firstChild).toHaveAccessibleName('Go to next page');
    expect(items[6].firstChild).toHaveAttribute('aria-disabled', 'true');
  });

});
