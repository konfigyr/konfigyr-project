import { FormattedMessage } from 'react-intl';

import type { ProfilePolicy } from '@konfigyr/hooks/types';

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

export function ProfilePolicyLabel({ value }: { value: ProfilePolicy }) {
  switch (value) {
    case 'UNPROTECTED':
      return (
        <FormattedMessage
          defaultMessage="Unprotected profile"
          description="Label text for unprotected profile"
        />
      );
    case 'PROTECTED':
      return (
        <FormattedMessage
          defaultMessage="Protected profile"
          description="Label text for protected profile"
        />
      );
    case 'IMMUTABLE':
      return (
        <FormattedMessage
          defaultMessage="Profile is read-only"
          description="Label text for immutable profile"
        />
      );
  }
}

export function ProfilePolicyDescription({ value }: { value: ProfilePolicy }) {
  switch (value) {
    case 'UNPROTECTED':
      return (
        <FormattedMessage
          defaultMessage="Changes to this profile will be directly applied without any review or approval."
          description="Description text for unprotected profile, stating that changes can be directly applied."
        />
      );
    case 'PROTECTED':
      return (
        <FormattedMessage
          defaultMessage="Changes to this profile require review and approval before being applied."
          description="Description text for protected profile, stating that changes requires review and approval before being applied."
        />
      );
    case 'IMMUTABLE':
      return (
        <FormattedMessage
          defaultMessage="No configuration changes may be applied to this profile. The existing configuration remains readable and auditable but cannot be modified."
          description="Description text for immutable profile, stating that profile is locked and cannot be edited."
        />
      );
  }
}

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
