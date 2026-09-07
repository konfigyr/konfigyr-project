import * as React from 'react';
import {
  Alert,
  AlertAction,
  AlertDescription,
  AlertTitle,
} from '@konfigyr/ui/components/alert';

import type { AlertProps } from '@konfigyr/ui/components/alert';

export function SimpleAlert({ icon, title, description, action, children, ...props }: {
  icon?: React.ReactNode,
  title?: React.ReactNode,
  description?: React.ReactNode,
  action?: React.ReactNode,
} & Omit<AlertProps, 'title'>) {
  const id = React.useId();
  const titleId = `${id}-alert-title`;
  const descriptionId = `${id}-alert-description`;

  return (
    <Alert
      {...props}
      aria-labelledby={title ? titleId : undefined}
      aria-describedby={description ? descriptionId : undefined}
    >
      {icon}

      {title && (
        <AlertTitle id={titleId}>{title}</AlertTitle>
      )}

      {description && (
        <AlertDescription id={descriptionId}>{description}</AlertDescription>
      )}

      {action && (
        <AlertAction>
          {action}
        </AlertAction>
      )}

      {children}
    </Alert>
  );
}
