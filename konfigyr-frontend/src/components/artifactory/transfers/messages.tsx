import { FormattedMessage } from 'react-intl';

export const TransfersLabel = () => (
  <FormattedMessage
    defaultMessage="Ownership transfers"
    description="Breadcrumb label for the ownership transfers list page."
  />
);

export const TransferDetailsLabel = () => (
  <FormattedMessage
    defaultMessage="Ownership transfer details"
    description="Label for the ownership transfers details page."
  />
);

export const GroupIdLabel = () => (
  <FormattedMessage
    defaultMessage="Group Id"
    description="Label for the Maven groupId of an ownership transfer."
  />
);

export const StateLabel = () => (
  <FormattedMessage
    defaultMessage="State"
    description="Label for the state of an ownership transfer."
  />
);

export const RequestedAtLabel = () => (
  <FormattedMessage
    defaultMessage="Requested at"
    description="Label for the date an ownership transfer was requested."
  />
);

export const ResolvedAtLabel = () => (
  <FormattedMessage
    defaultMessage="Resolved at"
    description="Label for the date an ownership transfer was resolved."
  />
);

export const CurrentOwnerLabel = () => (
  <FormattedMessage
    defaultMessage="Current owner"
    description="Label that shows the current owner of a groupId that is being transferred."
  />
);

export const RequestingNamespaceLabel = () => (
  <FormattedMessage
    defaultMessage="Request namespace"
    description="Label that shows the namespace that requested the groupId transfer."
  />
);

export const IncomingLabel = () => (
  <FormattedMessage
    defaultMessage="Incoming"
    description="Label for the toggle showing ownership transfers where this namespace is asked to release a groupId."
  />
);

export const OutgoingLabel = () => (
  <FormattedMessage
    defaultMessage="Outgoing"
    description="Label for the toggle showing ownership transfers this namespace has requested."
  />
);

export const RequestTransferLabel = () => (
  <FormattedMessage
    defaultMessage="Request transfer"
    description="Button label that starts a new ownership transfer request."
  />
);

export const AcceptTransferLabel = () => (
  <FormattedMessage
    defaultMessage="Accept and move artifacts"
    description="Button label that accepts an ownership transfer request."
  />
);

export const RejectTransferLabel = () => (
  <FormattedMessage
    defaultMessage="Reject"
    description="Button label that rejects an ownership transfer request."
  />
);

export const CancelTransferLabel = () => (
  <FormattedMessage
    defaultMessage="Cancel request"
    description="Button label that cancels a pending ownership transfer request."
  />
);

export const AcceptTransferTitle = () => (
  <FormattedMessage
    defaultMessage="Accept ownership transfer"
    description="Title of the confirmation dialog for accepting an ownership transfer request."
  />
);

export const RejectTransferTitle = () => (
  <FormattedMessage
    defaultMessage="Reject ownership transfer"
    description="Title of the confirmation dialog for rejecting an ownership transfer request."
  />
);

export const CancelTransferTitle = () => (
  <FormattedMessage
    defaultMessage="Cancel ownership transfer"
    description="Title of the confirmation dialog for canceling an ownership transfer request."
  />
);

export const AcceptTransferDescription = ({ groupId, to }: { groupId: string; to: string }) => (
  <FormattedMessage
    defaultMessage="Are you sure you want to transfer ownership of <b>{groupId}</b> to <b>{to}</b>?"
    values={{
      groupId,
      to,
      b: (chunks) => <strong>{chunks}</strong>,
    }}
    description="Confirmation text in the dialog for accepting an ownership transfer request."
  />
);

export const RejectTransferDescription = ({ groupId, to }: { groupId: string; to: string }) => (
  <FormattedMessage
    defaultMessage="Are you sure you want to reject the request from <b>{to}</b> to transfer ownership of <b>{groupId}</b>?"
    values={{
      groupId,
      to,
      b: (chunks) => <strong>{chunks}</strong>,
    }}
    description="Confirmation text in the dialog for rejecting an ownership transfer request."
  />
);

export const CancelTransferDescription = ({ groupId, from }: { groupId: string; from: string }) => (
  <FormattedMessage
    defaultMessage="Are you sure you want to cancel your request to transfer ownership of <b>{groupId}</b> from <b>{from}</b>?"
    values={{
      groupId,
      from,
      b: (chunks) => <strong>{chunks}</strong>,
    }}
    description="Confirmation text in the dialog for canceling an ownership transfer request."
  />
);

export const TransferAcceptedSuccessMessage = ({ groupId }: { groupId: string }) => (
  <FormattedMessage
    defaultMessage="Transferred ownership of {groupId}"
    values={{ groupId }}
    description="Success message when an ownership transfer request is accepted."
  />
);

export const TransferRejectedSuccessMessage = ({ groupId }: { groupId: string }) => (
  <FormattedMessage
    defaultMessage="Rejected the ownership transfer request for {groupId}"
    values={{ groupId }}
    description="Success message when an ownership transfer request is rejected."
  />
);

export const TransferCanceledSuccessMessage = ({ groupId }: { groupId: string }) => (
  <FormattedMessage
    defaultMessage="Canceled the ownership transfer request for {groupId}"
    values={{ groupId }}
    description="Success message when an ownership transfer request is canceled."
  />
);

export const TransferRequestedSuccessMessage = ({ groupId }: { groupId: string }) => (
  <FormattedMessage
    defaultMessage="Requested ownership transfer of {groupId}"
    values={{ groupId }}
    description="Success message when an ownership transfer request is created."
  />
);
