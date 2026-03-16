import * as React from 'react';
import {
  Pagination,
  PaginationContent,
  PaginationItem,
  PaginationNext,
  PaginationPrevious,
} from '@konfigyr/components/ui/pagination';

export type SimplePaginationProps = {
  /**
   * The current active page.
   */
  page: number;

  /**
   * The total number of pages available.
   */
  pages: number;

  /**
   * Callback function called when a page button is clicked.
   * Receives the new page number as an argument.
   */
  onClick: (page: number) => void;
};

export function SimplePagination({ page, pages, onClick }: SimplePaginationProps) {

  const handleNext = () => {
    if (page < pages) {
      onClick(page + 1);
    }
  };

  const handlePrevious = () => {
    if (page > 0) {
      onClick(page - 1);
    }
  };

  return (
    <Pagination>
      <PaginationContent>
        <PaginationItem>
          <PaginationPrevious
            onClick={handlePrevious}
            isActive={page !== 0}
            className={page === 0 ? 'pointer-events-none opacity-50' : 'cursor-pointer'}
          />
        </PaginationItem>

        <PaginationItem>
          <PaginationNext
            onClick={handleNext}
            isActive={page + 1 !== pages}
            className={page + 1 === pages ? 'pointer-events-none opacity-50' : 'cursor-pointer'}
          />
        </PaginationItem>
      </PaginationContent>
    </Pagination>
  );
}
