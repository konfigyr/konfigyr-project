import { FormattedMessage } from 'react-intl';
import ReactMarkdown from 'react-markdown';
import gfm from 'remark-gfm';
import rehypeAttrs from 'rehype-attr';
import rehypeHeadings from 'rehype-autolink-headings';
import rehypeIgnore from 'rehype-ignore';
import rehypePrism from 'rehype-prism-plus';
import rehypeRaw from 'rehype-raw';
import rehypeRewrite from 'rehype-rewrite';
import rehypeSlug from 'rehype-slug';
import { remarkAlert } from 'remark-github-blockquote-alert';
import { Contents } from '@konfigyr/components/ui/content';

import type { PluggableList } from 'unified';

const REMARK_PLUGINS: PluggableList = [
  remarkAlert,
  gfm,
];

const REHYPE_PLUGINS: PluggableList = [
  rehypeRaw,
  rehypeSlug,
  rehypeHeadings,
  rehypeIgnore,
  rehypeRewrite,
  rehypeAttrs,
  rehypePrism,
];

export function MarkdownPreview({ value }: { value?: string }) {
  return (
    <div
      role="note"
      aria-live="polite"
      data-slot="editor-preview"
      className="px-2.5 py-1 text-sm leading-relaxed text-foreground"
    >
      {(value && value.length > 0) ? (
        <Contents>
          <ReactMarkdown
            rehypePlugins={REHYPE_PLUGINS}
            remarkPlugins={REMARK_PLUGINS}
          >
            {value}
          </ReactMarkdown>
        </Contents>
      ): (
        <FormattedMessage
          tagName="i"
          defaultMessage="No preview available"
          description="Message displayed when there is no preview available"
        />
      )}
    </div>
  );
}
