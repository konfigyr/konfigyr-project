import { FormattedMessage } from 'react-intl';

export function NamespaceNameLabel() {
  return (
    <FormattedMessage
      defaultMessage="Name"
      description="Namespace name form label text"
    />
  );
}

export function NamespaceNameDescription() {
  return (
    <FormattedMessage
      defaultMessage="The official name of your namespace. This is how your team, project, or organization will be identified. Keep it clear and recognizable!"
      description="Text that is used as help text for the namespace name form field"
    />
  );
}

export function NamespaceSlugLabel() {
  return (
    <FormattedMessage
      defaultMessage="URL"
      description="Label text for the Namespace URL Slug"
    />
  );
}

export function NamespaceSlugDescription() {
  return (
    <FormattedMessage
      defaultMessage="The unique URL-friendly identifier for this namespace. Auto-generated from the name, but you can tweak it if needed. No spaces, just dashes!"
      description="Text that is used as help text for the Namespace URK Slug form field"
    />
  );
}

export function NamespaceDescriptionLabel() {
  return (
    <FormattedMessage
      defaultMessage="Description"
      description="Namespace description form label text"
    />
  );
}

export function NamespaceDescriptionHelpText() {
  return (
    <FormattedMessage
      defaultMessage="Tell the world (or just your team) what this namespace is all about. A short and sweet explanation of its purpose, goals, or mission. Or not..."
      description="Text that is used as help text for the namespace description form field"
    />
  );
}
