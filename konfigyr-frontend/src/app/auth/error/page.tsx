import type { OAuthError } from 'konfigyr/services/openid';
import ServiceLabel from 'konfigyr/components/service-label';
import OAuthErrorCard from 'konfigyr/components/error/oauth-error';

interface Parameters {
  searchParams: Promise<OAuthError & { code?: string }>
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

        <OAuthErrorCard {...error} />
      </div>
    </div>
  );
}
