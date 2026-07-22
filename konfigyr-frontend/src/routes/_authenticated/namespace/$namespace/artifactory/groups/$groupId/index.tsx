import { FormattedMessage } from 'react-intl';
import { ShieldCheckIcon } from 'lucide-react';
import { createFileRoute } from '@tanstack/react-router';
import {
  useGetGroupVerification,
  useGetVerificationChallenges,
  useNamespace,
} from '@konfigyr/hooks';
import { ErrorState } from '@konfigyr/components/error';
import { GroupsBreadcrumbs } from '@konfigyr/components/artifactory/groups/breadcrumbs';
import { EmptyState } from '@konfigyr/components/ui/empty';
import { GroupVerificationDetails } from '@konfigyr/components/artifactory/groups/group-verification-details';

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/artifactory/groups/$groupId/',
)({
  component: RouteComponent,
});

function RouteComponent () {
  const namespace = useNamespace();
  const { groupId } = Route.useParams();

  const {
    data: verification,
    error: verificationError,
    isError: isVerificationError,
    isPending: isVerificationPending,
  } = useGetGroupVerification(namespace.slug, groupId);

  const {
    data: challenges,
    error: challengesError,
    isError: isChallengesError,
    isPending: isChallengesPending,
  } = useGetVerificationChallenges(namespace.slug, groupId);

  const loading = isVerificationPending || isChallengesPending;
  const error = verificationError || challengesError;
  const isError = isVerificationError || isChallengesError;

  return (
    <>
      <GroupsBreadcrumbs namespace={namespace}>
        {groupId}
      </GroupsBreadcrumbs>

      <div className="mx-auto w-full space-y-6 lg:w-2/3 xl:w-3/5">
        {loading && (
          <EmptyState
            icon={<ShieldCheckIcon/>}
            title={(
              <FormattedMessage
                defaultMessage="Loading group claim"
                description="Loading state title for a group claim detail page."
              />
            )}
            description={(
              <FormattedMessage
                defaultMessage="Fetching the current claim and verification challenge."
                description="Loading state description for a group claim detail page."
              />
            )}
          />
        )}

        {isError && (
          <ErrorState error={error!} className="border-none"/>
        )}

        {verification && (
          <GroupVerificationDetails
            namespace={namespace}
            verification={verification}
            challenges={challenges?.data || []}
          />
        )}
      </div>
    </>
  );
}
