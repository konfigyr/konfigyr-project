'use client';

import { toast } from 'sonner';
import { useCallback } from 'react';
import { useRevokeGroupVerification } from '@konfigyr/hooks';
import { useErrorNotification } from '@konfigyr/components/error';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '@konfigyr/components/ui/alert-dialog';
import { CancelLabel } from '@konfigyr/components/messages';
import { TriangleAlert } from 'lucide-react';
import {
  CancelClaimLabel, CancelVerificationClaimDescription,
  CancelVerificationClaimTitle, ClaimCanceledSuccessMessage, ClaimRevokedSuccessMessage,
  RevokeClaimLabel, RevokeVerificationClaimDescription,
  RevokeVerificationClaimTitle,
} from '@konfigyr/components/groups/messages';
import type { ReactElement , ReactNode } from 'react';
import type { GroupVerification } from '@konfigyr/hooks/types';

export type RevokeGroupVerificationButtonProps = {
  namespace: string;
  verification: GroupVerification;
  action: 'CANCEL' | 'REVOKE'
  children: ReactElement
};

export function RevokeGroupVerificationButton({
  namespace,
  verification,
  action,
  children,
}: RevokeGroupVerificationButtonProps) {
  const errorNotification = useErrorNotification();
  const { isPending, mutateAsync: revokeGroupVerification } = useRevokeGroupVerification(namespace);

  const submitLabel: ReactNode = action === 'CANCEL' ? <CancelClaimLabel /> : <RevokeClaimLabel />;

  const title: ReactNode = action === 'CANCEL' ? <CancelVerificationClaimTitle /> : <RevokeVerificationClaimTitle /> ;

  const description: ReactNode = action === 'CANCEL' ?
    <CancelVerificationClaimDescription groupId={verification.groupId} /> :
    <RevokeVerificationClaimDescription groupId={verification.groupId} /> ;

  const successMessage: ReactNode = action === 'CANCEL' ?
    <ClaimCanceledSuccessMessage groupId={verification.groupId} /> :
    <ClaimRevokedSuccessMessage groupId={verification.groupId} /> ;

  const onRevoke = useCallback(async () => {
    try {
      await revokeGroupVerification(verification.groupId);
    } catch (error) {
      return errorNotification(error);
    }

    toast.success(successMessage);
  }, [verification, errorNotification, revokeGroupVerification]);

  return (
    <AlertDialog>
      <AlertDialogTrigger render={( children )} />
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle className="flex items-center gap-2">
            <TriangleAlert className="h-5 w-5 text-destructive shrink-0" />
            {( title )}
          </AlertDialogTitle>
          <AlertDialogDescription>
            {( description )}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>
            <CancelLabel />
          </AlertDialogCancel>
          <AlertDialogAction variant="destructive" disabled={isPending} loading={isPending} onClick={onRevoke}>
            {( submitLabel )}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
