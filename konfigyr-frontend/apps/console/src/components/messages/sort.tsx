import { FormattedMessage } from 'react-intl';

export function SortByLabel() {
  return (
    <FormattedMessage
      defaultMessage="Sort by"
      description="Label used as a heading, or a placeholder, for the sort by dropdown."
    />
  );
}

export function SortByNameAscending() {
  return (
    <FormattedMessage
      defaultMessage="Name ascending"
      description="Label for the sort by name in an ascending direction option."
    />
  );
}

export function SortByNameDescending() {
  return (
    <FormattedMessage
      defaultMessage="Name descending"
      description="Label for the sort by name in an descending direction option."
    />
  );
}

export function SortByLatest() {
  return (
    <FormattedMessage
      defaultMessage="Latest"
      description="Label for the latest sort option. This should force the page to load the latest resources first from the server."
    />
  );
}

export function SortByOldest() {
  return (
    <FormattedMessage
      defaultMessage="Oldest"
      description="Label for the oldest sort option. This should force the page to load the oldest resources first from the server."
    />
  );
}

export function SortByMostRecentlyUpdated() {
  return (
    <FormattedMessage
      defaultMessage="Recently updated"
      description="Label for the most recently updated option. This should force the page to load the most recently updated resources first from the server."
    />
  );
}

export function SortByLeastRecentlyUpdated() {
  return (
    <FormattedMessage
      defaultMessage="Least recently updated"
      description="Label for the least recently updated option. This should force the page to load the least recently updated resources first from the server."
    />
  );
}
