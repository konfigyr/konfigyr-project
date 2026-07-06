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