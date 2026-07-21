import { useMemo } from 'react';
import { FormattedMessage } from 'react-intl';
import { PackageIcon, RotateCcwIcon, ShieldBanIcon, ShieldCheckIcon, XIcon } from 'lucide-react';
import { Link, createFileRoute } from '@tanstack/react-router';
import {
  useGetGroupVerification,
  useGetVerificationChallenges,
  useNamespace,
} from '@konfigyr/hooks';
import { ErrorState } from '@konfigyr/components/error';
import { GroupsBreadcrumbs } from '@konfigyr/components/artifactory/groups/breadcrumbs';
import { ConflictingOwnersAlert, GroupVerificationStateAlert } from '@konfigyr/components/artifactory/groups/group-verification-state';
import { Button, buttonVariants } from '@konfigyr/components/ui/button';
import { EmptyState } from '@konfigyr/components/ui/empty';
import { GroupVerification } from '@konfigyr/components/artifactory/groups/group-verification-challenge';
import {
  CancelClaimLabel,
  ClaimAgainLabel,
  RevokeClaimLabel,
  VerifyClaimLabel,
} from '@konfigyr/components/artifactory/groups/messages';
import {
  CancelGroupVerificationButton,
  RevokeGroupVerificationButton,
} from '@konfigyr/components/artifactory/groups/revoke-group-verification';
import { VerifyGroupVerificationButton } from '@konfigyr/components/artifactory/groups/verify-group-verification';

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

  const challenge = useMemo(() => {
    const items = challenges?.data ?? [];
    // items already ordered on the backend by createdAt.asc(),
    return items.at(-1);
  }, [challenges]);

  const banner = verification
    ? <GroupVerificationStateAlert groupId={verification.groupId} verificationChallenge={challenge} verificationState={verification.state}/>
    : null;

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
          <>
            <div className="flex items-start justify-between gap-4">
              <div className="flex min-w-0 items-center gap-3">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-xl border bg-card">
                  <PackageIcon className="size-5 text-muted-foreground" aria-hidden="true"/>
                </div>
                <div className="min-w-0">
                  <h1 className="truncate font-mono text-2xl font-medium leading-tight">
                    {verification.groupId}
                  </h1>
                </div>
              </div>
            </div>

            {banner}

            {!!verification.conflictingOwners?.length && (
              <ConflictingOwnersAlert conflictingOwners={verification.conflictingOwners}/>
            )}

            <GroupVerification verification={verification} challenge={challenge}/>

            <div className="flex flex-wrap gap-3">
              {verification.state === 'ACTIVE' && (
                <RevokeGroupVerificationButton namespace={namespace.slug} verification={verification} >
                  <Button variant="destructive">
                    <ShieldBanIcon />
                    <RevokeClaimLabel/>
                  </Button>
                </RevokeGroupVerificationButton>
              )}

              {verification.state === 'PENDING' && (
                <CancelGroupVerificationButton namespace={namespace.slug} verification={verification} >
                  <Button variant="destructive">
                    <XIcon/>
                    <CancelClaimLabel />
                  </Button>
                </CancelGroupVerificationButton>
              )}

              {(verification.state === 'PENDING' || verification.state === 'FAILED') && (
                <VerifyGroupVerificationButton namespace={namespace.slug} verification={verification}>
                  <Button>
                    <RotateCcwIcon/>
                    <VerifyClaimLabel/>
                  </Button>
                </VerifyGroupVerificationButton>
              )}

              {verification.state === 'REVOKED' && (
                <Link
                  to="/namespace/$namespace/artifactory/groups/$groupId/edit"
                  params={{ namespace: namespace.slug, groupId }}
                  className={buttonVariants({ variant: 'default' })}
                >
                  <RotateCcwIcon/>
                  <ClaimAgainLabel/>
                </Link>
              )}
            </div>
          </>
        )}
      </div>
    </>
  );
}
