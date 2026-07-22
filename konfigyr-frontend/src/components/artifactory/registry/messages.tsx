import { FormattedMessage } from 'react-intl';

export const RegistryLabel = () => (
  <FormattedMessage
    defaultMessage="Artifact registry"
    description="Breadcrumb and sidebar label for the artifact registry list page."
  />
);

export const VisibilityLabel = () => (
  <FormattedMessage
    defaultMessage="Visibility"
    description="Label for the visibility of an artifact."
  />
);

export const PublicLabel = () => (
  <FormattedMessage
    defaultMessage="Public"
    description="Label for a PUBLIC artifact visibility."
  />
);

export const PrivateLabel = () => (
  <FormattedMessage
    defaultMessage="Private"
    description="Label for a PRIVATE artifact visibility."
  />
);

export const VersionsLabel = () => (
  <FormattedMessage
    defaultMessage="Versions"
    description="Label for the list of published versions of an artifact."
  />
);

export const PublishedAtLabel = () => (
  <FormattedMessage
    defaultMessage="Published at"
    description="Label for the date an artifact version was published."
  />
);

export const WebsiteLabel = () => (
  <FormattedMessage
    defaultMessage="Website"
    description="Label for the website URL of an artifact."
  />
);

export const RepositoryLabel = () => (
  <FormattedMessage
    defaultMessage="Repository"
    description="Label for the source control repository URL of an artifact."
  />
);

export const MakePublicLabel = () => (
  <FormattedMessage
    defaultMessage="Make public"
    description="Button label that changes an artifact's visibility to PUBLIC."
  />
);

export const MakePrivateLabel = () => (
  <FormattedMessage
    defaultMessage="Make private"
    description="Button label that changes an artifact's visibility to PRIVATE."
  />
);

export const ChangeVisibilityTitle = () => (
  <FormattedMessage
    defaultMessage="Change artifact visibility"
    description="Title of the confirmation dialog for changing an artifact's visibility."
  />
);

export const MakePublicDescription = ({ artifact }: { artifact: string }) => (
  <FormattedMessage
    defaultMessage="Are you sure you want to make <b>{artifact}</b> public? Every namespace will be able to read its metadata and property definitions."
    values={{ artifact, b: (chunks) => <strong>{chunks}</strong> }}
    description="Confirmation text in the dialog for making an artifact PUBLIC."
  />
);

export const MakePrivateDescription = ({ artifact }: { artifact: string }) => (
  <FormattedMessage
    defaultMessage="Are you sure you want to make <b>{artifact}</b> private? Only this namespace will be able to read its metadata and property definitions."
    values={{ artifact, b: (chunks) => <strong>{chunks}</strong> }}
    description="Confirmation text in the dialog for making an artifact PRIVATE."
  />
);

export const VisibilityChangedSuccessMessage = ({ artifact }: { artifact: string }) => (
  <FormattedMessage
    defaultMessage="Updated the visibility of {artifact}"
    values={{ artifact }}
    description="Success message shown after an artifact's visibility was changed."
  />
);

export const NoArtifactsFoundTitle = () => (
  <FormattedMessage
    defaultMessage="No artifacts found"
    description="Empty state title when no artifacts are found in the registry list."
  />
);

export const NoArtifactsFoundDescription = () => (
  <FormattedMessage
    defaultMessage="This namespace has not published any artifact metadata yet."
    description="Empty state description when no artifacts are found in the registry list."
  />
);

export const NoVersionsFoundTitle = () => (
  <FormattedMessage
    defaultMessage="No versions found"
    description="Empty state title when an artifact has no published versions."
  />
);

export const SearchPropertiesPromptTitle = () => (
  <FormattedMessage
    defaultMessage="Search this version's properties"
    description="Empty state title shown before a property search term has been entered on the version detail page."
  />
);

export const SearchPropertiesPromptDescription = () => (
  <FormattedMessage
    defaultMessage="Type a property name or description to see matching configuration properties published with this version."
    description="Empty state description shown before a property search term has been entered on the version detail page."
  />
);

export const NoMatchingPropertiesTitle = () => (
  <FormattedMessage
    defaultMessage="No matching properties found"
    description="Empty state title when no properties match the search term on the version detail page."
  />
);
