import { FormattedMessage } from 'react-intl';

export function ServiceNameLabel() {
  return (
    <FormattedMessage
      defaultMessage="Display name"
      description="The form label used in the service forms to define the service display name input field"
    />
  );
}

export function ServiceDescriptionLabel() {
  return (
    <FormattedMessage
      defaultMessage="Description"
      description="The form label used in the service forms to define the service description input field"
    />
  );
}

export function ServiceDescriptionHelpText() {
  return (
    <FormattedMessage
      defaultMessage="Add any internal notes about this service."
      description="The form help text used in the service forms to define the service description input field"
    />
  );
}

export function ArtifactsLabel() {
  return (
    <FormattedMessage
      defaultMessage="Artifacts"
      description="The label used to name the service artifacts, mainly used in the navigation."
    />
  );
}

export function ArtifactLabel() {
  return (
    <FormattedMessage
      defaultMessage="Artifact"
      description="The label used to name the single service artifact, mainly used in labels or prefixes."
    />
  );
}

export function ConfigurationPropertiesLabel() {
  return (
    <FormattedMessage
      defaultMessage="Configuration properties"
      description="The label used to name the service configuration properties, mainly used in the navigation."
    />
  );
}

export function MissingManifestsDescription() {
  return (
    <FormattedMessage
      defaultMessage="This usually means that the service has not yet published its dependency manifest to Konfigyr."
      description="Empty state description used when no configuration properties are present in the service manifest."
    />
  );
}

export function ServiceManifestsInstructions() {
  return (
    <FormattedMessage
      defaultMessage="To create the manifest, execute a release using the Konfigyr build plugin in the service repository. The plugin will upload the artifact dependency information required to generate the service manifest."
      description="Help text describing how service manifests are created"
    />
  );
}
