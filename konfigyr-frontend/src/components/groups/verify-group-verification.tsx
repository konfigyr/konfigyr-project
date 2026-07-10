'use client';

import { toast } from 'sonner';
import { useCallback, useState } from 'react';
import { useVerifyGroupVerification } from '@konfigyr/hooks';
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
  ClaimVerifiedSuccessMessage,
  ClaimVerifiedWarningMessage,
  VerifyClaimLabel,
} from '@konfigyr/components/groups/messages';
import { FormattedMessage } from 'react-intl';
import type { ReactElement } from 'react';
import type { GroupVerification } from '@konfigyr/hooks/types';

type VerifyGroupVerificationButtonProps = {
  namespace: string;
  verification: GroupVerification;
  children: ReactElement;
};

export function VerifyGroupVerificationButton ({
  namespace,
  verification,
  children,
}: VerifyGroupVerificationButtonProps) {
  const errorNotification = useErrorNotification();
  const { isPending, mutateAsync: verifyGroupVerification } = useVerifyGroupVerification(namespace);
  const [open, setOpen] = useState(false);

  const handleOpenChange = useCallback((nextOpen: boolean) => {
    setOpen(nextOpen);
  }, []);

  const onVerify = useCallback(async () => {
    try {
      const { state } = await verifyGroupVerification(verification.groupId);
      if (state !== 'ACTIVE') {
        toast.warning(<ClaimVerifiedWarningMessage />);
        return;
      }
      toast.success(<ClaimVerifiedSuccessMessage />);
    } catch (error) {
      return errorNotification(error);
    } finally {
      handleOpenChange(false);
    }

  }, [errorNotification, handleOpenChange, verification.groupId, verifyGroupVerification]);

  return (
    <AlertDialog open={open} onOpenChange={handleOpenChange}>
      <AlertDialogTrigger render={children}/>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle className="flex items-center gap-2">
            <TriangleAlert className="h-5 w-5 text-primary shrink-0"/>
            <FormattedMessage
              defaultMessage="Verify group verification claim"
              description="Title of the confirmation dialog for verifying a group verification claim."
            />
          </AlertDialogTitle>
          <AlertDialogDescription>
            <FormattedMessage
              defaultMessage="Are you sure you want to verify the group verification claim for {groupId}?"
              values={{
                groupId: verification.groupId,
              }}
              description="Confirmation text in the dialog for verifying a group verification claim."
            />
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>
            <CancelLabel/>
          </AlertDialogCancel>
          <AlertDialogAction disabled={isPending} loading={isPending} onClick={onVerify}>
            <VerifyClaimLabel/>
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
