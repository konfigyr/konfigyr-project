import { FormattedMessage } from 'react-intl';
import {
  ChangeRequestReviewType,
  ChangeRequestState,
} from '@konfigyr/hooks/vault/types';

export function DiscardChangeRequestReviewLabel() {
  return (
    <FormattedMessage
      defaultMessage="Discard change request"
      description="Label for the discard change request review button."
    />
  );
}

export function SubmitChangeRequestReviewLabel() {
  return (
    <FormattedMessage
      defaultMessage="Submit review"
      description="Label for the change request review submit button."
    />
  );
}

export function ChangeRequestReviewTypeLabel({ type }: { type: ChangeRequestReviewType }) {
  switch (type) {
    case ChangeRequestReviewType.APPROVE:
      return (
        <FormattedMessage
          defaultMessage="Approve changes"
          description="Label for the approve change request review type"
        />
      );
    case ChangeRequestReviewType.REQUEST_CHANGES:
      return (
        <FormattedMessage
          defaultMessage="Request changes"
          description="Label for the request changes change request review type"
        />
      );
    case ChangeRequestReviewType.COMMENT:
      return (
        <FormattedMessage
          defaultMessage="Comment"
          description="Label for the comment change request review type"
        />
      );
  }
}

export function ChangeRequestReviewTypeDescription({ type }: { type: ChangeRequestReviewType }) {
  switch (type) {
    case ChangeRequestReviewType.APPROVE:
      return (
        <FormattedMessage
          defaultMessage="Submit feedback and approve merging these changes."
          description="Description for the approve change request review type"
        />
      );
    case ChangeRequestReviewType.REQUEST_CHANGES:
      return (
        <FormattedMessage
          defaultMessage="Submit feedback suggesting changes."
          description="Description for the request changes change request review type"
        />
      );
    case ChangeRequestReviewType.COMMENT:
      return (
        <FormattedMessage
          defaultMessage="Submit general feedback without explicit approval."
          description="Description for the comment change request review type"
        />
      );
  }
}

export function ChangeRequestStateLabel({ value }: { value: ChangeRequestState }) {
  switch (value) {
    case ChangeRequestState.OPEN:
      return (
        <FormattedMessage
          defaultMessage="Open"
          description="Label for the change request opened state."
        />
      );
    case ChangeRequestState.MERGED:
      return (
        <FormattedMessage
          defaultMessage="Merged"
          description="Label for the change request merged state."
        />
      );
    case ChangeRequestState.DISCARDED:
      return (
        <FormattedMessage
          defaultMessage="Discarded"
          description="Label for the change request discared state."
        />
      );
  }
}
