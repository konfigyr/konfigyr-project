import type { OAuthError } from 'konfigyr/services/openid';
import Link from 'next/link';
import { useTranslations } from 'next-intl';
import ServiceLabel from 'konfigyr/components/service-label';
import { ErrorCodes } from 'konfigyr/services/openid';

interface Parameters {
  searchParams: Promise<OAuthError & { code?: string }>
}

export function ErrorCard(error: OAuthError & { code?: string }) {
  const t = useTranslations();
  const code = error?.code || ErrorCodes.INTERNAL_SERVER_ERROR;

  return (
    <div>
      <div className="mb-4">
        <h2 className="text-xl">{t('errors.oauth.title')}</h2>
        <p className="text-sm">{t(`errors.oauth.code.${code}`)}</p>
      </div>
      <div>
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
            <a href={error.error_uri} target='_blank'>
              {t('errors.oauth.link')}
            </a>
          </div>
        )}
      </div>
      <div className="flex justify-between mt-4">
        <Link href="/auth/authorize">
          {t('authentication.login')}
        </Link>
        <a href="mailto:support@konfigyr.com" target="_blank">
          {t('errors.contact-support')}
        </a>
      </div>
    </div>
  );
}

/**
 * The authentication error page where the authentication route would redirect the users in case
 * of OAuth2 errors.
 * <p>
 * The page reads the error information from the URL query parameters which must contain the
 * error code and, if known, the {@link OAuthError} parameters obtained from the Authorization server.
 *
 * @param searchParams the URL query parameters
 * @constructor
 */
export default async function OAuthErrorPage({searchParams}: Parameters) {
  const error = await searchParams;

  return (
    <div className="container px-4">
      <div className="w-full md:w-1/2 lg:w-1/3 mx-auto">
        <h1 className="text-center text-2xl mb-4">
          <ServiceLabel/>
        </h1>

        <ErrorCard {...error} />
      </div>
    </div>
  );
}
