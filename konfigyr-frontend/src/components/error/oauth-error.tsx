import Link from 'next/link';
import { useTranslations } from 'next-intl';
import { SquareArrowOutUpRight, Headset } from 'lucide-react';
import { ErrorCodes, OAuthError } from 'konfigyr/services/openid';
import { Button, Card, CardHeader, CardContent, CardFooter } from 'konfigyr/components/ui';

/**
 * OAuth error card component used to display the details of the `OAuthError` to the end user.
 *
 * @param error OAuth error details
 * @constructor
 */
export default function OAuthErrorCard(error: OAuthError & { code?: string }) {
  const t = useTranslations();
  const code = error?.code || ErrorCodes.INTERNAL_SERVER_ERROR;

  return (
    <Card>
      <CardHeader
        title={t('errors.oauth.title')}
        description={t(`errors.oauth.code.${code}`)}
      />
      <CardContent>
        {error.error && (
          <p className="text-sm">
            <span>Error code:</span> <code>{error.error}</code>
          </p>
        )}

        {error.error_description && (
          <p className="text-sm text-muted-foreground">
            <span>Error description:</span> {error.error_description}
          </p>
        )}

        {error.error_uri && (
          <div className="pt-4">
            <Button variant="ghost" asChild>
              <a href={error.error_uri} target='_blank'>
                {t('errors.oauth.link')} <SquareArrowOutUpRight size={8} />
              </a>
            </Button>
          </div>
        )}
      </CardContent>
      <CardFooter className="justify-between">
        <Button asChild>
          <Link href="/auth/authorize">
            {t('authentication.login')}
          </Link>
        </Button>
        <Button variant="outline" asChild>
          <a href="mailto:support@konfigyr.com" target="_blank">
            <Headset /> {t('errors.contact-support')}
          </a>
        </Button>
      </CardFooter>
    </Card>
  );
}
