import { createContext, useContext, useMemo } from 'react';
import { FormattedMessage } from 'react-intl';
import {
  ChevronLeftIcon,
  ChevronRightIcon,
  MoreHorizontalIcon,
} from 'lucide-react';
import { cn } from '@konfigyr/components/utils';
import { buttonVariants } from '@konfigyr/components/ui/button';

import type { ComponentProps, ReactNode } from 'react';
import type { Button } from '@konfigyr/components/ui/button';

export type PaginationProps = {
  /**
   * Property that defines the current page size. Defaults to 10.
   */
  size?: number

  /**
   * Property that defines the current page. Defaults to 1.
   */
  page?: number;

  /**
   * Property that defines the total number of results. Defaults to 0.
   */
  total?: number;

  /**
   * Property that defines the total number of pages. Defaults to 1.
   */
  pages?: number;
};

const PaginationContext = createContext<PaginationProps>({
  size: 10,
  page: 1,
  total: 0,
  pages: 1,
});

function Pagination({ page, size, total, pages, className, ...props }: PaginationProps & ComponentProps<'nav'>) {
  return (
    <PaginationContext.Provider value={{ page, size, total, pages }}>
      <nav
        role='navigation'
        aria-label='pagination'
        data-slot='pagination'
        className={cn('mx-auto flex w-full justify-center', className)}
        {...props}
      />
    </PaginationContext.Provider>
  );
}

function PaginationContent({
  className,
  ...props
}: ComponentProps<'ul'>) {
  return (
    <ul
      data-slot='pagination-content'
      className={cn('flex flex-row items-center gap-1', className)}
      {...props}
    />
  );
}

type Page = { page: number, active: boolean };

export function PaginationRange({ range = 5, children }: {
  range?: number,
  children: (state: Page) => ReactNode | null | undefined,
}) {
  const { page = 1, pages = 1 } = useContext(PaginationContext);

  const items = useMemo(() => {
    const state: Array<Page> = [];

    // the current page is the first one
    if (page === 1) {
      for (let i = 1; i < range + 1; i++) {
        i < pages + 1 && state.push({ page: i, active: i === page });
      }
    }
    // the current page is the last one
    else if (page >= pages) {
      for (let i = pages - range + 1; i <= pages; i++) {
        i > 0 && state.push({ page: i, active: i === page });
      }
    }
    // the current page is somewhere in the middle of the pagination range
    else {
      const visible: Array<number> = [];

      // determine page range limits
      const lowerLimit = page - Math.floor((range - 1) / 2);
      const upperLimit = page + Math.ceil((range - 1) / 2);

      for (let i = lowerLimit; i <= upperLimit; i++) {
        // if page is too low shift the range by one page up, if possible
        if (i < 1 && upperLimit + 1 <= pages) {
          visible.push(upperLimit + 1);
        }
        // if page is too high shift the range by one page down, if possible
        else if (i > pages && lowerLimit - 1 > 0) {
          visible.unshift(lowerLimit - 1);
        }
        // if the page is allowed, add it at the proper index
        else {
          visible.splice(i - 1, 0, i);
        }
      }

      visible.forEach(i => {
        state.push({ page: i, active: i === page });
      });
    }

    return state;
  }, [page, pages, range]);

  return items.map(state => (
    <PaginationItem key={state.page}>
      {children(state)}
    </PaginationItem>
  ));
}

function PaginationItem({ ...props }: ComponentProps<'li'>) {
  return <li data-slot='pagination-item' {...props} />;
}

type PaginationLinkProps = { isActive?: boolean } & Pick<ComponentProps<typeof Button>, 'size'> & ComponentProps<'a'>;

function PaginationLink({
  className,
  isActive,
  size = 'icon',
  ...props
}: PaginationLinkProps) {
  return (
    <a
      aria-current={isActive ? 'page' : undefined}
      data-slot='pagination-link'
      data-active={isActive}
      className={cn(
        'cursor-pointer',
        isActive && 'w-128',
        buttonVariants({
          variant: isActive ? 'outline' : 'ghost',
          size,
        }),
        className,
      )}
      {...props}
    />
  );
}

function PaginationPrevious({
  className,
  ...props
}: ComponentProps<typeof PaginationLink>) {
  const { page = 1 } = useContext(PaginationContext);
  const disabled = page === 1;

  return (
    <PaginationLink
      aria-disabled={disabled}
      aria-label='Go to previous page'
      size='default'
      tabIndex={disabled ? -1 : undefined}
      className={cn('gap-1 px-2.5 sm:pl-2.5', disabled && 'pointer-events-none opacity-50', className)}
      {...props}
    >
      <ChevronLeftIcon />
      <span className='hidden sm:block'>
        <FormattedMessage
          defaultMessage="Previous"
          description="Previous page label"
        />
      </span>
    </PaginationLink>
  );
}

function PaginationNext({
  className,
  ...props
}: ComponentProps<typeof PaginationLink>) {
  const { page = 1, pages = 1 } = useContext(PaginationContext);
  const disabled = page === pages;

  return (
    <PaginationLink
      aria-disabled={disabled}
      aria-label='Go to next page'
      size='default'
      tabIndex={disabled ? -1 : undefined}
      className={cn('gap-1 px-2.5 sm:pr-2.5', disabled && 'pointer-events-none opacity-50', className)}
      {...props}
    >
      <span className='hidden sm:block'>
        <FormattedMessage
          defaultMessage="Next"
          description="Next page label"
        />
      </span>
      <ChevronRightIcon />
    </PaginationLink>
  );
}

function PaginationEllipsis({
  className,
  ...props
}: ComponentProps<'span'>) {
  return (
    <span
      aria-hidden
      data-slot='pagination-ellipsis'
      className={cn('flex size-9 items-center justify-center', className)}
      {...props}
    >
      <MoreHorizontalIcon className='size-4' />
      <span className='sr-only'>
        <FormattedMessage
          defaultMessage="More pages"
          description="More pages label"
        />
      </span>
    </span>
  );
}

export {
  Pagination,
  PaginationContent,
  PaginationLink,
  PaginationItem,
  PaginationPrevious,
  PaginationNext,
  PaginationEllipsis,
};
