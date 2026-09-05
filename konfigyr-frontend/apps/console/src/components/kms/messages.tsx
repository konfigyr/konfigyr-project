import { FormattedMessage } from 'react-intl';

export function KeyManagementServiceLabel() {
  return (
    <FormattedMessage
      defaultMessage="Key management"
      description="Key management service (KMS) label text, used for page titles or navigation links."
    />
  );
}

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

export function KeysetRotationIntervalLabel() {
  return (
    <FormattedMessage
      defaultMessage="Rotation interval"
      description="KMS keyset rotation interval label text, used in forms or table headers."
    />
  );
}

export function KeysetRotationIntervalHelpText() {
  return (
    <FormattedMessage
      defaultMessage="How often the primary key is automatically replaced with a new one."
      description="KMS keyset rotation interval help text, used in forms or table headers."
    />
  );
}

export function KeysetDeletionGracePeriodLabel() {
  return (
    <FormattedMessage
      defaultMessage="Deletion grace period"
      description="KMS keyset deletion grace period label text, used in forms or table headers."
    />
  );
}

export function KeysetDeletionGracePeriodHelpText() {
  return (
    <FormattedMessage
      defaultMessage="How long a key stays pending deletion before its material is permanently erased."
      description="KMS keyset deletion grace period help text, used in forms or table headers."
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

export function KeyDeactivateLabel() {
  return (
    <FormattedMessage
      defaultMessage="Disable key"
      description="Label used on action links or buttons that would disable the key within a KMS keyset."
    />
  );
}

export function KeyReactivateLabel() {
  return (
    <FormattedMessage
      defaultMessage="Reactivate key"
      description="Label used on action links or buttons that would reactivate the disabled key within a KMS keyset."
    />
  );
}

export function KeyCompromisedLabel() {
  return (
    <FormattedMessage
      defaultMessage="Mark as compromised"
      description="Label used on action links or buttons that would mark the key in the KMS keyset as compromised."
    />
  );
}

export function KeyRestoreLabel() {
  return (
    <FormattedMessage
      defaultMessage="Restore key"
      description="Label used on action links or buttons that would restore the key scheduled for destruction."
    />
  );
}

export function KeyDestroyLabel() {
  return (
    <FormattedMessage
      defaultMessage="Schedule for destruction"
      description="Label used on action links or buttons that would either destroy KMS key or schedule its destruction."
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
