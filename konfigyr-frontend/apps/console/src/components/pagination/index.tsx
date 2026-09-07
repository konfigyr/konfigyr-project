import { useCallback } from 'react';
import { Link } from '@tanstack/react-router';
import { NextLabel, PreviousLabel } from '@konfigyr/components/messages';
import {
  Pagination,
  PaginationContent,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
  PaginationRange,
} from '@konfigyr/ui/components/pagination';

import type { ComponentProps } from 'react';
import type { PageResponse } from '@konfigyr/hateoas';

export type CursorPaginationProps = {
  /**
   * Property that defines the current page size. Defaults to 10.
   */
  size?: number;

  /**
   * The cursor token of the next page.
   */
  next?: string | null;

  /**
   * The cursor token of the previous page.
   */
  previous?: string | null;

  /**
   * Callback function that is called when the user clicks on the next or previous page button.
   * @param token the selected cursor token
   * @param size the selected page size
   */
  onChange: (token: string, size: number) => unknown;
};

export function CursorPagination({
  size = 10,
  next,
  previous,
  onChange,
  className,
  ...props
}: CursorPaginationProps & Omit<ComponentProps<'nav'>, 'onChange'>) {
  const onNext = useCallback(() => {
    if (typeof next === 'string') {
      onChange(next, size);
    }
  }, [size, next]);

  const onPrevious = useCallback(() => {
    if (typeof previous === 'string') {
      onChange(previous, size);
    }
  }, [size, previous]);

  return (
    <Pagination size={size} className={className} {...props}>
      <PaginationContent>
        <PaginationItem>
          <PaginationPrevious
            label={<PreviousLabel />}
            disabled={typeof previous !== 'string'}
            onClick={onPrevious}
          />
        </PaginationItem>
        <PaginationItem>
          <PaginationNext
            label={<NextLabel />}
            disabled={typeof next !== 'string'}
            onClick={onNext}
          />
        </PaginationItem>
      </PaginationContent>
    </Pagination>
  );
}

export function PageResponsePagination<T>({ page = 1, size = 20, response, ...props }: {
  page?: number;
  size?: number;
  response?: PageResponse<T>;
} & ComponentProps<'nav'>) {
  const pages = response?.metadata.pages || 1;
  const total = response?.metadata.total || 0;

  if (pages < 2) {
    return null;
  }

  return (
    <Pagination page={page} pages={pages} total={total} size={size} {...props}>
      <PaginationContent>
        <PaginationItem>
          <PaginationPrevious
            label={<PreviousLabel />}
            render={(
              <Link to="." search={search => ({ ...search, page: page - 1 })}/>
            )}
          />
        </PaginationItem>
        <PaginationRange>
          {(state) => (
            <PaginationLink
              isActive={state.active}
              render={(
                <Link to="." search={search => ({ ...search, page: state.page })}>
                  {state.page}
                </Link>
              )}
            />
          )}
        </PaginationRange>
        <PaginationItem>
          <PaginationNext
            label={<NextLabel />}
            render={(
              <Link to="." search={search => ({ ...search, page: page + 1 })}/>
            )}
          />
        </PaginationItem>
      </PaginationContent>
    </Pagination>
  );
}
