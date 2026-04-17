import { FormattedMessage } from 'react-intl';
import { ChangeRequestState } from '@konfigyr/hooks/vault/types';
import { RelativeDate } from '@konfigyr/components/messages';
import { ChangesCountLabel } from '@konfigyr/components/vault/messages';

import type { ChangeRequest } from '@konfigyr/hooks/vault/types';

export function ChangeRequestStateSummary({ changeRequest }: { changeRequest: ChangeRequest }) {
  switch (changeRequest.state) {
    case ChangeRequestState.OPEN:
      return (
        <FormattedMessage
          defaultMessage="{author} wants to perform {changes} in {profile} configuration profile."
          description="Summary label that is used for open change requests that explain the intent of the change request author."
          values={{
            author: <span className="font-medium">{changeRequest.createdBy}</span>,
            changes: <span className="font-medium"><ChangesCountLabel count={changeRequest.count} /></span>,
            profile: <code>{changeRequest.profile.slug}</code>,
          }}
        />
      );
    case ChangeRequestState.MERGED:
      return (
        <FormattedMessage
          defaultMessage="{author} merged {changes} into {profile} configuration profile {time}."
          description="Summary label that is used for merged change requests that explain how, when and where change request was merged."
          values={{
            author: changeRequest.createdBy,
            changes: <ChangesCountLabel count={changeRequest.count} />,
            profile: <code>{changeRequest.profile.slug}</code>,
            time: <RelativeDate value={changeRequest.updatedAt} />,
          }}
        />
      );
    case ChangeRequestState.DISCARDED:
      return (
        <FormattedMessage
          defaultMessage="{author} discarded {changes} {time}."
          description="Summary label that is used for discared change requests that explain who and when discarded the change request."
          values={{
            author: changeRequest.createdBy,
            changes: <ChangesCountLabel count={changeRequest.count} />,
            time: <RelativeDate value={changeRequest.updatedAt} />,
          }}
        />
      );
  }
}
