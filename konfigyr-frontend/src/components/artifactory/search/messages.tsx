import { FormattedMessage } from 'react-intl';

export const PropertySearchLabel = () => (
  <FormattedMessage
    defaultMessage="Property search"
    description="Breadcrumb and sidebar label for the artifact property search page."
  />
);

export const OwnedByLabel = ({ owner }: { owner: string }) => (
  <FormattedMessage
    defaultMessage="Owned by {owner}"
    description="Label used to describe the owner of a property."
    values={{ owner: <span className="font-semibold">{owner}</span> }}
  />
);

export const NoMatchingPropertiesTitle = () => (
  <FormattedMessage
    defaultMessage="No matching properties found"
    description="Empty state title when no properties match the search term on the version detail page."
  />
);
