import { FormattedMessage, defineMessage } from 'react-intl';
import { Link } from '@tanstack/react-router';
import { Button } from '@konfigyr/ui/components/button';

import type { MessageDescriptor } from 'react-intl';
import type { ButtonProps } from '@konfigyr/ui/components/button';

export const callToActionMessage = defineMessage({
  defaultMessage: 'Request Early Access',
  description: 'The call to action label to request early access.',
});

export function CallToActionLink({ label = callToActionMessage, ...props }: { label?: MessageDescriptor } & ButtonProps) {
  return (
    <Button {...props} render={<Link to="/early-access" />}>
      <FormattedMessage {...callToActionMessage} />
    </Button>
  );
}
