import { FormattedMessage } from 'react-intl';
import {
  CircleCheckBigIcon,
  FolderPenIcon,
  GitMergeIcon,
  GitPullRequestClosedIcon,
  GitPullRequestCreateIcon,
  MessageCircleIcon,
  MessageCirclePlusIcon,
} from 'lucide-react';
import { ChangeRequestHistoryType } from '@konfigyr/hooks/vault/types';
import { RelativeDate } from '@konfigyr/components/messages';
import { HtmlContents } from '@konfigyr/components/ui/content';

import type { ChangeRequestHistory } from '@konfigyr/hooks/vault/types';

function ChangeRequestTimelineIcon({ history, className }: { history: ChangeRequestHistory, className: string }) {
  switch (history.type) {
    case ChangeRequestHistoryType.CREATED:
      return (
        <GitPullRequestCreateIcon className={className} />
      );
    case ChangeRequestHistoryType.APPROVED:
      return (
        <CircleCheckBigIcon className={className} />
      );
    case ChangeRequestHistoryType.COMMENTED:
      return (
        <MessageCircleIcon className={className} />
      );
    case ChangeRequestHistoryType.CHANGES_REQUESTED:
      return (
        <MessageCirclePlusIcon className={className} />
      );

    case ChangeRequestHistoryType.RENAMED:
      return (
        <FolderPenIcon className={className} />
      );
    case ChangeRequestHistoryType.MERGED:
      return (
        <GitMergeIcon className={className} />
      );
    case ChangeRequestHistoryType.DISCARDED:
      return (
        <GitPullRequestClosedIcon className={className} />
      );
  }
}

function ChangeRequestTimelineSummary({ history }: { history: ChangeRequestHistory }) {
  switch (history.type) {
    case ChangeRequestHistoryType.CREATED:
      return (
        <FormattedMessage
          defaultMessage="{author} opened the change request {time}."
          description="Summary label that is used for open change requests that explain the intent of the change request author."
          values={{
            author: <span className="font-medium">{history.initiator}</span>,
            time: <RelativeDate value={history.timestamp} />,
          }}
        />
      );
    case ChangeRequestHistoryType.APPROVED:
      return (
        <FormattedMessage
          defaultMessage="{author} approved this change request {time}."
          description="Summary label that is used for open change requests that explain the intent of the change request author."
          values={{
            author: <span className="font-medium">{history.initiator}</span>,
            time: <RelativeDate value={history.timestamp} />,
          }}
        />
      );
    case ChangeRequestHistoryType.COMMENTED:
      return (
        <FormattedMessage
          defaultMessage="{author} commented on this change request {time}."
          description="Summary label that is used for open change requests that explain the intent of the change request author."
          values={{
            author: <span className="font-medium">{history.initiator}</span>,
            time: <RelativeDate value={history.timestamp} />,
          }}
        />
      );
    case ChangeRequestHistoryType.CHANGES_REQUESTED:
      return (
        <FormattedMessage
          defaultMessage="{author} requested additional changes {time}."
          description="Summary label that is used for open change requests that explain the intent of the change request author."
          values={{
            author: <span className="font-medium">{history.initiator}</span>,
            time: <RelativeDate value={history.timestamp} />,
          }}
        />
      );

    case ChangeRequestHistoryType.RENAMED:
      return (
        <FormattedMessage
          defaultMessage="{author} renamed this change request {time}."
          description="Summary label that is used for open change requests that explain the intent of the change request author."
          values={{
            author: <span className="font-medium">{history.initiator}</span>,
            time: <RelativeDate value={history.timestamp} />,
          }}
        />
      );
    case ChangeRequestHistoryType.MERGED:
      return (
        <FormattedMessage
          defaultMessage="{author} merged this change request {time}."
          description="Summary label that is used for open change requests that explain the intent of the change request author."
          values={{
            author: <span className="font-medium">{history.initiator}</span>,
            time: <RelativeDate value={history.timestamp} />,
          }}
        />
      );
    case ChangeRequestHistoryType.DISCARDED:
      return (
        <FormattedMessage
          defaultMessage="{author} discared changes {time}."
          description="Summary label that is used for open change requests that explain the intent of the change request author."
          values={{
            author: <span className="font-medium">{history.initiator}</span>,
            time: <RelativeDate value={history.timestamp} />,
          }}
        />
      );
  }
}

function ChangeRequestTimelineItem({ history, isLast }: { history: ChangeRequestHistory, isLast: boolean }) {
  return (
    <li className="group/timeline-item relative flex gap-2">
      {!isLast && (
        <div className="absolute left-3.75 top-4 bottom-0 w-px bg-border" />
      )}

      <span className="relative shrink-0 size-8 rounded-full flex items-center justify-center bg-muted">
        <ChangeRequestTimelineIcon history={history} className="size-4" />
      </span>

      <div className="flex-1 pt-2 pb-8 min-w-0 group-last/timeline-item:pb-0">
        <div className="flex items-baseline justify-between gap-2 text-xs">
          <span><ChangeRequestTimelineSummary history={history} /></span>
        </div>
        {history.comment && (
          <div className="px-4 py-2 mt-2 rounded-md border text-sm">
            <HtmlContents html={history.comment.html} />
          </div>
        )}
      </div>
    </li>
  );
}

export function ChangeRequestTimeline({ history }: { history: Array<ChangeRequestHistory> }) {
  return (
    <ul>
      {history.map((record, index) => (
        <ChangeRequestTimelineItem
          key={record.id}
          history={record}
          isLast={index === history.length - 1}
        />
      ))}
    </ul>
  );
}
