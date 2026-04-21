import { useEffect, useState } from 'react';
import production from 'react/jsx-runtime';
import { unified } from 'unified';
import rehypeParse from 'rehype-parse';
import rehypePrism from 'rehype-prism-plus';
import { toJsxRuntime } from 'hast-util-to-jsx-runtime';
import { cn } from '@konfigyr/components/utils';

import type { ComponentProps, ReactElement } from 'react';

export function Contents({ className, children, ...props }: { className?: string } & ComponentProps<'div'>) {
  return (
    <div
      data-slot="contents"
      className={cn(
        // heading styles
        '[&_h1]:text-[1.375rem] [&_h1]:font-medium [&_h1]:font-heading [&_h1]:leading-snug [&_h1]:mb-3 [&_h1]:pb-2 [&_h1]:border-b [&_h1]:border-border',
        '[&_h2]:text-lg [&_h2]:font-medium [&_h2]:font-heading [&_h2]:leading-snug [&_h2]:mb-2 [&_h2]:pb-1.5 [&_h2]:border-b [&_h2]:border-border',
        '[&_h3]:text-[0.9375rem] [&_h3]:font-medium [&_h3]:font-heading [&_h3]:leading-snug [&_h3]:mt-6 [&_h3]:mb-1.5',
        '[&_h4]:text-sm [&_h4]:font-medium [&_h4]:font-heading [&_h4]:leading-snug [&_h4]:mt-5 [&_h4]:mb-1.25',
        '[&_h5]:text-[0.8125rem] [&_h5]:font-medium [&_h5]:font-heading [&_h5]:leading-snug [&_h5]:mt-4 [&_h5]:mb-1',
        '[&_h6]:text-[0.8125rem] [&_h6]:font-medium [&_h6]:font-heading [&_h6]:leading-snug [&_h6]:mt-4 [&_h6]:mb-1',
        // paragraph styles
        '[&_p]:mt-0 [&_p]:mb-3.5',
        '[&_strong]:font-medium [&_strong]:text-foreground',
        '[&_em]:italic',
        // code styles
        // horizontal rule styles
        '[&_hr]:border-none [&_hr]:border-t [&_hr]:border-border [&_hr]:my-5',
        // image styles
        '[&_img]:max-w-full [&_img]:h-auto [&_img]:rounded-md [&_img]:my-2',
        // link styles
        '[&_a]:text-primary [&_a]:underline [&_a]:underline-offset-[3px] [&_a]:decoration-transparent [&_a:hover]:decoration-primary [&_a]:transition-colors',
        // list styles
        '[&_ul]:mt-0 [&_ul]:mb-3.5 [&_ul]:pl-6 [&_ul]:list-disc',
        '[&_ol]:mt-0 [&_ol]:mb-3.5 [&_ol]:pl-6 [&_ol]:list-decimal',
        '[&_li]:mt-1 [&_li]:mb-1',
        '[&_li>ul]:mt-1 [&_li>ul]:mb-1 [&_li>ol]:mt-1 [&_li>ol]:mb-1',
        // blockquote styles
        '[&_blockquote]:mt-0 [&_blockquote]:mb-3.5 [&_blockquote]:pl-4 [&_blockquote]:border-l-2 [&_blockquote]:border-border [&_blockquote]:text-muted-foreground [&_blockquote]:italic',
        '[&_blockquote>p]:mb-0',
        // table styles
        '[&_table]:w-full [&_table]:border-collapse [&_table]:text-[0.8125rem] [&_table]:mb-3.5',
        '[&_thead]:border-b [&_thead]:border-border',
        '[&_th]:text-left [&_th]:font-medium [&_th]:px-3 [&_th]:py-1.5 [&_th]:bg-muted [&_th]:text-foreground',
        '[&_td]:px-3 [&_td]:py-1.5 [&_td]:border-b [&_td]:border-border [&_td]:text-foreground',
        '[&_tbody_tr:last-child_td]:border-b-0',
        '[&>*:first-child]:mt-0 [&>*:last-child]:mb-0',
        className,
      )}
      {...props}
    >
      {children}
    </div>
  );
}

const processor = unified()
  .use(rehypeParse, { fragment: true })
  .use(rehypePrism);

export function HtmlContents({ html, ...props }: { html: string } & ComponentProps<typeof Contents>) {
  const [contents, setContents] = useState<ReactElement | null>(null);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    let cancelled = false;

    processor.run(processor.parse(html), (err, tree) => {
      if (!cancelled) {
        setError(err ?? null);

        if (tree) {
          setContents(toJsxRuntime(tree, {
            ...production,
            ignoreInvalidStyle: true,
            passKeys: true,
            passNode: true,
          }));
        }
      }
    });

    return () => {
      cancelled = true;
    };
  }, [html]);

  if (error) {
    throw error;
  }

  return (
    <Contents {...props}>
      {contents}
    </Contents>
  );
}
