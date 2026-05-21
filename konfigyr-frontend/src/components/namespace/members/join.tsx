import { FormattedMessage } from 'react-intl';
import { ArrowRightIcon } from 'lucide-react';
import { RelativeDate } from '@konfigyr/components/messages';
import { Button } from '@konfigyr/components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@konfigyr/components/ui/card';

import type { Invitation } from '@konfigyr/hooks/types';

export function JoinNamespace({ invitation, onAccept }: { invitation: Invitation, onAccept: () => void }) {
  return (
    <Card className="border">
      <CardHeader className="border-b">
        <p className="mb-2 font-mono text-sm font-medium uppercase text-muted-foreground/80">
          <FormattedMessage
            defaultMessage="You've been invited"
            description="Eyebrow raising label used on the namespace join page. Should state that user has been invited."
          />
        </p>
        <CardTitle className="text-2xl tracking-tight">
          <FormattedMessage
            defaultMessage="Join the {namespace} organization"
            description="Title that is shown on the namespace join page when user is invited to join a namespace"
            values={{ namespace: (<strong>{invitation.organization.name}</strong>) }}
          />
        </CardTitle>
        <CardDescription>
          <FormattedMessage
            defaultMessage="{sender} has a seat waiting for you."
            description="Label on the namespace join page that informs the invitation recipient about an available membership spot."
            values={{ sender: invitation.sender.name }}
          />
        </CardDescription>
      </CardHeader>
      <CardContent>
        <p className="mb-4 text-sm leading-relaxed">
          <FormattedMessage
            defaultMessage="{sender} has invited you to collaborate inside the {namespace} namespace, your shared space for configuration management, secrets, and operational governance."
            description="Text on the namespace join page that informs the user about invitation to a namespace and who sent it. It includes sender's name and namespace name."
            values={{
              sender: (<span className="font-medium text-foreground">{invitation.sender.name}</span>),
              namespace: (<span className="font-medium text-foreground">{invitation.organization.name}</span>),
            }}
          />
        </p>

        <Button className="w-full tracking-wide" size="lg" onClick={onAccept}>
          <FormattedMessage
            defaultMessage="Join {namespace}"
            description="The button label that is shown on the namespace join page when user is invited to join a namespace. It includes namespace name."
            values={{ namespace: invitation.organization.name }}
          />
          <ArrowRightIcon />
        </Button>

        <p className="mt-3 text-muted-foreground" role="note">
          <FormattedMessage
            defaultMessage="This invitation expires in {expiry}. Join before then"
            description="Invitation expiration message that is shown on the namespace join page when user is invited to join a namespace. It includes expiration time."
            values={{ expiry: (<RelativeDate value={invitation.expiryDate} />) }}
          />
        </p>

      </CardContent>
      <CardFooter className="bg-card border-t flex-col items-start">
        <p className="text-sm text-muted-foreground/80 italic">
          <FormattedMessage
            defaultMessage="See you inside, {sender}, {namespace} team."
            description="Join namespace footer message that is shown on the namespace join page when user is invited to join a namespace. It includes sender's name and namespace name."
            values={{
              sender: (<><br/><span className="font-medium text-foreground">{invitation.sender.name}</span></>),
              namespace: (<span className="font-medium text-foreground/80">{invitation.organization.name}</span>),
            }}
          />
        </p>
      </CardFooter>
    </Card>
  );
}
