import {
  createContext,
  useContext,
  useMemo,
} from 'react';
import { mergeProps } from '@base-ui/react/merge-props';
import { useRender } from '@base-ui/react/use-render';
import {
  ChevronLeftIcon,
  ChevronRightIcon,
  MoreHorizontalIcon,
} from 'lucide-react';
import { cn } from '@konfigyr/ui/lib/utils';
import { buttonVariants } from '@konfigyr/ui/components/button';

import type { ComponentProps, ReactNode } from 'react';
import type { Button } from '@konfigyr/ui/components/button';

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

export const usePagination = () => useContext(PaginationContext);

export function Pagination({ page, size, total, pages, className, ...props }: PaginationProps & ComponentProps<'nav'>) {
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

export function PaginationContent({
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

export type Page = { page: number, active: boolean };

export function PaginationRange({ range = 5, children }: {
  range?: number,
  children: (state: Page) => ReactNode | null | undefined,
}) {
  const { page = 1, pages = 1 } = usePagination();

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
      const lowerLimit = Math.max(page - Math.floor((range - 1) / 2), 1);
      const upperLimit = Math.min(page + Math.ceil((range - 1) / 2), pages);

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

export function PaginationItem({ ...props }: ComponentProps<'li'>) {
  return <li data-slot='pagination-item' {...props} />;
}

export type PaginationLinkProps = {
  isActive?: boolean;
  disabled?: boolean;
  href?: string
} & Pick<ComponentProps<typeof Button>, 'size'> & useRender.ComponentProps<'button'>;

export function PaginationLink({
  className,
  isActive = false,
  size = 'icon',
  disabled = false,
  render,
  ...props
}: PaginationLinkProps) {
  return useRender({
    defaultTagName: props.href ? 'a' : 'button',
    props: mergeProps<'a' | 'button'>({
      'aria-current': isActive ? 'page' : undefined,
      'aria-disabled': disabled,
      tabIndex: disabled ? -1 : undefined,
      className: cn(
        buttonVariants({
          variant: isActive ? 'outline' : 'ghost',
          size,
        }),
        disabled && 'pointer-events-none opacity-50',
        !isActive && 'text-foreground',
        'cursor-pointer',
        className,
      ),
    }, props),
    render,
    state: {
      slot: 'pagination-link',
      active: String(isActive),
    },
  });
}

export function PaginationPrevious({
  size = 'default',
  className,
  label = 'Previous',
  ...props
}: ComponentProps<typeof PaginationLink> & { label?: ReactNode }) {
  const { page = 1 } = usePagination();
  const disabled = props.disabled ?? page === 1;

  return (
    <PaginationLink
      size={size}
      disabled={disabled}
      aria-label='Go to previous page'
      className={cn('gap-1 px-2.5 sm:pl-2.5', className)}
      {...props}
    >
      <ChevronLeftIcon />
      <span className='hidden sm:block'>{label}</span>
    </PaginationLink>
  );
}

export function PaginationNext({
  size = 'default',
  className,
  label = 'Next',
  ...props
}: ComponentProps<typeof PaginationLink> & { label?: ReactNode }) {
  const { page = 1, pages = 1 } = usePagination();
  const disabled = props.disabled ?? page === pages;

  return (
    <PaginationLink
      size={size}
      disabled={disabled}
      aria-label='Go to next page'
      className={cn('gap-1 px-2.5 sm:pr-2.5', className)}
      {...props}
    >
      <span className='hidden sm:block'>{label}</span>
      <ChevronRightIcon />
    </PaginationLink>
  );
}

export function PaginationEllipsis({
  className,
  label = 'More pages',
  ...props
}: ComponentProps<'span'> & { label?: ReactNode }) {
  return (
    <span
      aria-hidden
      data-slot='pagination-ellipsis'
      className={cn('flex size-9 items-center justify-center', className)}
      {...props}
    >
      <MoreHorizontalIcon className='size-4' />
      <span className='sr-only'>{label}</span>
    </span>
  );
}
