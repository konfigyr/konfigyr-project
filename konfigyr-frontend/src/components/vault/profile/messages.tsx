import { FormattedMessage } from 'react-intl';

export const ProfileNameLabel = () => (
  <FormattedMessage
    defaultMessage="Profile name"
    description="Label used to provide a label a profile display name. Used mainly in profile forms or table headers."
  />
);

export const ProfileNameHelpText = () => (
  <FormattedMessage
    defaultMessage="A human-friendly name used to identify this profile in the interface. Must be between 2 and 30 characters."
    description="Label used to provide additional information about the profile display name. Used mainly in profile forms."
  />
);

export const ProfileSlugLabel = () => (
  <FormattedMessage
    defaultMessage="Profile identifier"
    description="Label used to provide a label a profile URL slug. Used mainly in profile forms or table headers."
  />
);

export const ProfileSlugHelpText = () => (
  <FormattedMessage
    defaultMessage="A unique identifier of the profile. Only lowercase letters, numbers, and hyphens are allowed."
    description="Label used to provide additional information how the profile slug is formatted. Used mainly in profile forms."
  />
);

export const ProfileDescriptionLabel = () => (
  <FormattedMessage
    defaultMessage="Description"
    description="Label used to provide a label a profile description field. Used mainly in profile forms."
  />
);

export const ProfileDescriptionHelpText = () => (
  <FormattedMessage
    defaultMessage="Provide additional context about when and how this profile should be used. Maximum 255 characters."
    description="Label used to provide additional information about the profile description. Used mainly in profile forms."
  />
);

export const ProfilePolicyLabel = () => (
  <FormattedMessage
    defaultMessage="Profile policy"
    description="Label used to provide a label for a profile policy. Used mainly in profile forms."
  />
);

export const ProfilePositionLabel = () => (
  <FormattedMessage
    defaultMessage="Display order"
    description="Label used to provide a label for a profile display order or position. Used mainly in profile forms."
  />
);

export const ProfilePositionHelpText = () => (
  <FormattedMessage
    defaultMessage="Determines the position of this profile in the profile tab list. Lower numbers appear first."
    description="Label used to provide additional information about the profile display order. Used mainly in profile forms."
  />
);
