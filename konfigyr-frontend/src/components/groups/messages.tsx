import { FormattedMessage } from 'react-intl';

export const GroupIdLabel = () => (
  <FormattedMessage
    defaultMessage="Group Id"
    description="Label for the Maven groupId of a group verification claim."
  />
);

export const StateLabel = () => (
  <FormattedMessage
    defaultMessage="Verification state"
    description="Label for the state of a group verification claim."
  />
);

export const ChallengeStateLabel = () => (
  <FormattedMessage
    defaultMessage="Challenge state"
    description="Label for the verification challenge state."
  />
);

export const VerifiedAtLabel = () => (
  <FormattedMessage
    defaultMessage="Verified at"
    description="Label for the date a group verification claim was verified."
  />
);

export const RevokedAtLabel = () => (
  <FormattedMessage
    defaultMessage="Revoked at"
    description="Label for the claim revocation timestamp."
  />
);

export const ExpiresAtLabel = () => (
  <FormattedMessage
    defaultMessage="Expires at"
    description="Label for the verifacition challenge expiration timestamp."
  />
);

export const MethodLabel = () => (
  <FormattedMessage
    defaultMessage="Verification method"
    description="Label for the verifacition challenge method."
  />
);

export const EditClaimLabel = () => (
  <FormattedMessage
    defaultMessage="Edit group id claim"
    description="Label for the page title when editing a group ID claim."
  />
);

export const RevokeClaimLabel = () => (
  <FormattedMessage
    defaultMessage="Revoke claim"
    description="Button label that revokes an active group claim."
  />
);

export const CancelClaimLabel = () => (
  <FormattedMessage
    defaultMessage="Cancel claim"
    description="Button label that cancels an pending group claim."
  />
);

export const RevokeVerificationClaimTitle = () => (
  <FormattedMessage
    defaultMessage="Revoke group verification claim"
    description="Title of the confirmation dialog for revoking a group verification claim"
  />
);

export const CancelVerificationClaimTitle = () => (
  <FormattedMessage
    defaultMessage="Cancel group verification claim"
    description="Title of the confirmation dialog for canceling a group verification claim"
  />
);

export const RevokeVerificationClaimDescription = ({ groupId }: { groupId: string } ) => (
  <FormattedMessage
    defaultMessage="Are you sure you want to revoke the group verification claim for <b>{groupId}</b>?"
    values={{
      groupId,
      b: (chunks) => <strong>{chunks}</strong>,
    }}
    description="Confirmation text in the dialog for revoking a group verification claim"
  />
);

export const CancelVerificationClaimDescription = ({ groupId }: { groupId: string } ) => (
  <FormattedMessage
    defaultMessage="Are you sure you want to cancel the group verification claim for <b>{groupId}</b>?"
    values={{
      groupId,
      b: (chunks) => <strong>{chunks}</strong>,
    }}
    description="Confirmation text in the dialog for canceling a group verification claim"
  />
);

export const ClaimRevokedSuccessMessage = ({ groupId }: { groupId: string }) => (
  <FormattedMessage
    defaultMessage="Successfully revoked {groupId}"
    values={{ groupId }}
    description="Success message when a group verification claim is revoked"
  />
);

export const ClaimCanceledSuccessMessage = ({ groupId }: { groupId: string }) => (
  <FormattedMessage
    defaultMessage="Successfully canceled {groupId}"
    values={{ groupId }}
    description="Success message when a group verification claim is revoked"
  />
);

export const ClaimAgainLabel = () => (
  <FormattedMessage
    defaultMessage="Claim again"
    description="Button label that opens the edit page after revocation."
  />
);

export const VerifyClaimLabel = () => (
  <FormattedMessage
    defaultMessage="Verify claim"
    description="Button label that verifies a group claim."
  />
);

export const ClaimVerifiedSuccessMessage = () => (
  <FormattedMessage
    defaultMessage="Claim verified successfully."
    description="Success message when a group verification claim is verified."
  />
);

export const ClaimVerifiedWarningMessage = () => (
  <FormattedMessage
    defaultMessage="Cannot verify claim. Please check configuration and try again."
    description="Warning message when a group verification claim does not end up active after verification."
  />
);

export const GroupClaimsLabel = () => (
  <FormattedMessage
    defaultMessage="Group claims"
    description="Breadcrumb label for the group verification claims list page."
  />
);
