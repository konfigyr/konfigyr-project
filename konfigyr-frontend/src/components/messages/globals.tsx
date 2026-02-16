import { FormattedMessage } from 'react-intl';

import type { ComponentProps } from 'react';

export const KonfigyrTitleMessage = () => (
  <FormattedMessage
    defaultMessage="Konfigyr"
    description="The title of the Konfigyr application"
  />
);

export const KonfigyrLeadMessage = () => (
  <FormattedMessage
    defaultMessage="Configuration made easy."
    description="The lead message of the Konfigyr application"
  />
);

export const GeneralErrorTitle = () => (
  <FormattedMessage
    defaultMessage="Unexpected server error occurred"
    description="Used to provide a default error message title"
  />
);

export const GeneralErrorDetail = () => (
  <FormattedMessage
    defaultMessage="We could not process your request due to a runtime error. Please try again and if the problem persists, please get in touch with our support team."
    description="Used to provide a default error message detail"
  />
);

export const GeneralErrorLink = ({ children, ...props }: ComponentProps<'a'>) => (
  <a target="_blank" rel="noopener noreferrer" {...props}>
    <FormattedMessage
      defaultMessage="Find out more here"
      description="Used as a label for a link that leads to the error documentation"
    />
    {children}
  </a>
);

export const OAuthErrorTitle = () => (
  <FormattedMessage
    defaultMessage="Authorization error"
    description="Used to provide a default OAuth error message title"
  />
);

export const OAuthErrorDetail = () => (
  <FormattedMessage
    defaultMessage="Unexpected server error occurred while logging you in. Please try again and if the problem persists, please get in touch with our support team."
    description="Used to provide a default OAuth error message detail"
  />
);

export const ContactSupport = () => (
  <FormattedMessage
    defaultMessage="Contact our support team"
    description="The label for a link that leads to the support page"
  />
);

export function CreatedAtLabel() {
  return (
    <FormattedMessage
      defaultMessage="Created at"
      description="Label used to describe a resource's creation date."
    />
  );
}

export function UpdatedAtLabel() {
  return (
    <FormattedMessage
      defaultMessage="Updated at"
      description="Label used to describe a resource's last update date."
    />
  );
}

export const ActionsLabel = () => (
  <FormattedMessage
    defaultMessage="Actions"
    description="The label used mainly for screen reader purposes. It is used mainly in table header cells where the action buttons are located."
  />
);

export const CancelLabel = () => (
  <FormattedMessage
    defaultMessage="Cancel"
    description="The label used for cancel buttons or links."
  />
);

export const HistoryLabel = () => (
  <FormattedMessage
    defaultMessage="History"
    description="The label used for links that lead to the resource's history page. It can also be used as a heading for the history page itself."
  />
);

export const CopyLabel = () => (
  <FormattedMessage
    defaultMessage="Copy"
    description="Default label for the clipboard button, usually used to copy some text to the clipboard."
  />
);

export const CopiedLabel = () => (
  <FormattedMessage
    defaultMessage="Copied!"
    description="Label for the clipboard button notifying the user that the text has been successfully added to the clipboard."
  />
);

export const CloseLabel = () => (
  <FormattedMessage
    defaultMessage="Close"
    description="The label used for close buttons."
  />
);

export const EditLabel = () => (
  <FormattedMessage
    defaultMessage="Edit"
    description="The label used for edit buttons or links."
  />
);

export const SaveLabel = () => (
  <FormattedMessage
    defaultMessage="Save"
    description="The label used for save buttons."
  />
);

export const UndoLabel = () => (
  <FormattedMessage
    defaultMessage="Undo"
    description="The label used for undo buttons or links."
  />
);

export const DeleteLabel = () => (
  <FormattedMessage
    defaultMessage="Delete"
    description="The label used for delete buttons or links."
  />
);

export const SearchLabel = () => (
  <FormattedMessage
    defaultMessage="Search"
    description="The label used for search inputs or buttons."
  />
);
