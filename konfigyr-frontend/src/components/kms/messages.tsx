import { FormattedMessage } from 'react-intl';

export function CreateKeysetLabel() {
  return (
    <FormattedMessage
      defaultMessage="Create keyset"
      description="Create KMS keyset namespace label text"
    />
  );
}

export function KeysetNameLabel() {
  return (
    <FormattedMessage
      defaultMessage="Keyset name"
      description="KMS keyset name label text, used in forms or table headers."
    />
  );
}

export function KeysetAlgorithmLabel() {
  return (
    <FormattedMessage
      defaultMessage="Algorithm"
      description="KMS keyset algorithm label text, used in forms or table headers."
    />
  );
}

export function KeysetStateLabel() {
  return (
    <FormattedMessage
      defaultMessage="State"
      description="KMS keyset state label text, used in forms or table headers."
    />
  );
}

export function KeysetRotateLabel() {
  return (
    <FormattedMessage
      defaultMessage="Rotate keyset"
      description="Label used on action links or buttons that would perform a rotation of a KMS keyset."
    />
  );
}

export function KeysetEncryptLabel() {
  return (
    <FormattedMessage
      defaultMessage="Encrypt data"
      description="Label used on action links or buttons that would perform a KMS keyset encrypt operation."
    />
  );
}

export function KeysetDecryptLabel() {
  return (
    <FormattedMessage
      defaultMessage="Decrypt ciphertext"
      description="Label used on action links or buttons that would perform a KMS keyset decrypt operation."
    />
  );
}

export function KeysetSignLabel() {
  return (
    <FormattedMessage
      defaultMessage="Create signature"
      description="Label used on action links or buttons that would perform a KMS keyset signing operation."
    />
  );
}

export function KeysetVerifySignatureLabel() {
  return (
    <FormattedMessage
      defaultMessage="Verify signature"
      description="Label used on action links or buttons that would perform a KMS keyset signature verification operation."
    />
  );
}

export function KeysetDisableLabel() {
  return (
    <FormattedMessage
      defaultMessage="Disable keyset"
      description="Label used on action links or buttons that would disable the KMS keyset."
    />
  );
}

export function KeysetReactivateLabel() {
  return (
    <FormattedMessage
      defaultMessage="Reactivate keyset"
      description="Label used on action links or buttons that would reactivate the disabled KMS keyset."
    />
  );
}

export function KeysetDestroyLabel() {
  return (
    <FormattedMessage
      defaultMessage="Destroy"
      description="Label used on action links or buttons that would destroy the KMS keyset."
    />
  );
}

export function KeysetDestroyedAtLabel() {
  return (
    <FormattedMessage
      defaultMessage="Destroyed at"
      description="Lable used to describe a KMS keyset's destruction date."
    />
  );
}
