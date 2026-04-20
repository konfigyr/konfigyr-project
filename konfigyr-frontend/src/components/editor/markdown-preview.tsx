import { useMemo } from 'react';
import { FormattedMessage } from 'react-intl';
import { Marked } from 'marked';
import { markedHighlight } from 'marked-highlight';
import hljs from 'highlight.js';
import { cn } from '@konfigyr/components/utils';
import 'highlight.js/styles/github.min.css';

import { useEditorState } from './context';

const engine = new Marked(markedHighlight({
  emptyLangClass: 'hljs',
  langPrefix: 'hljs language-',
  highlight(code, lang) {
    const language = hljs.getLanguage(lang) ? lang : 'plaintext';
    return hljs.highlight(code, { language }).value;
  },
}));

function PreviewContents({ markdown }: { markdown: string }) {
  const html = useMemo(() => engine.parse(markdown) as string, [markdown]);

  return (
    <div
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
        '[&_pre]:bg-muted [&_pre]:border [&_pre]:border-border [&_pre]:rounded-md',
        '[&_code]:font-mono [&_code]:text-xs [&_code]:bg-muted [&_code]:px-1 [&_code]:py-0.5 [&_code]:rounded',
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
      )}
      dangerouslySetInnerHTML={{ __html: html }}
    />
  );
}

export function MarkdownPreview() {
  const { value } = useEditorState();

  return (
    <div
      role="note"
      aria-live="polite"
      data-slot="editor-preview"
      className="px-2.5 py-1 text-sm leading-relaxed text-foreground"
    >
      {(value && value.length > 0) ? (
        <PreviewContents markdown={value} />
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
