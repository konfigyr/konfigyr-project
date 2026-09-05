import { FormattedMessage } from 'react-intl';

export function NamespaceOverviewModuleLabel() {
  return (
    <FormattedMessage
      defaultMessage="Namespace management"
      description="Label for the namespace management module switcher option"
    />
  );
}

export function VaultModuleLabel() {
  return (
    <FormattedMessage
      defaultMessage="Vault"
      description="Label for the vault module switcher option"
    />
  );
}

export function ArtifactoryModuleLabel() {
  return (
    <FormattedMessage
      defaultMessage="Artifactory"
      description="Label for the artifactory module switcher option"
    />
  );
}


export function KeyManagementSystemModuleLabel() {
  return (
    <FormattedMessage
      defaultMessage="KMS"
      description="Label for the KMS module switcher option"
    />
  );
}

export function PublicKeyInfrastructureModuleLabel() {
  return (
    <FormattedMessage
      defaultMessage="PKI"
      description="Label for the PKI module switcher option"
    />
  );
}
