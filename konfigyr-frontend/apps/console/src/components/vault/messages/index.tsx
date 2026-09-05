import { FormattedMessage, useIntl } from 'react-intl';
import { PropertyTransitionType } from '@konfigyr/hooks/vault/types';

import type { IntlShape } from 'react-intl';

export function ChangesCountLabel({ count }: { count: number }) {
  return (
    <FormattedMessage
      defaultMessage="{count, plural, =0 {No changes} one {# change} other {# changes}}"
      description="Change history count label. Uses the plural form of the count, accepts the count as a variable."
      values={{ count }}
    />
  );
}

export const useLabelForTransitionType = (type: PropertyTransitionType) => {
  const intl = useIntl();
  return labelForTransitionType(intl, type);
};

export const labelForTransitionType = (intl: IntlShape, type: PropertyTransitionType) => {
  switch (type) {
    case PropertyTransitionType.ADDED:
      return intl.formatMessage({
        defaultMessage: 'Added',
        description: 'Label for added property transition',
      });
    case PropertyTransitionType.UPDATED:
      return intl.formatMessage({
        defaultMessage: 'Modified',
        description: 'Label for modified property transition',
      });
    case PropertyTransitionType.REMOVED:
      return intl.formatMessage({
        defaultMessage: 'Deleted',
        description: 'Label for deleted property transition',
      });
    default:
      throw new Error(`Unknown transition type: ${type}`);
  }
};
