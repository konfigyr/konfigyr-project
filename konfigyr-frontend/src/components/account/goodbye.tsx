import { HandIcon } from 'lucide-react';
import { FormattedMessage } from 'react-intl';
import { Card, CardContent, CardHeader, CardIcon, CardTitle } from '@konfigyr/components/ui/card';
import { Separator } from '@konfigyr/components/ui/separator';

export function Goodbye() {
  return (
    <div className="h-screen flex justify-center items-center">
      <div className="w-full md:w-1/2 lg:w-1/3 mx-auto">
        <Card className="border">
          <CardHeader className="flex items-center gap-2">
            <CardIcon>
              <HandIcon size="1.5rem" />
            </CardIcon>
            <CardTitle>
              <FormattedMessage
                defaultMessage="Account successfully deleted"
                description="Title that is shown on the account goodbye page when user successfully deleted his account"
              />
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <FormattedMessage
              defaultMessage="Your account and its data have been securely removed. We really appreciate the time you spent building, testing, and shipping with us."
              description="Text that is shown on the account goodbye page when user successfully deleted his account"
              tagName="p"
            />

            <Separator />

            <FormattedMessage
              defaultMessage="If you ever feel like starting fresh, you can spin up a new account anytime. Until then, best of luck with your next project."
              description="Text that is shown on the account goodbye page when user successfully deleted his account"
              tagName="p"
            />
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
