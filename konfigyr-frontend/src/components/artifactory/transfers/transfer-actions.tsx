'use client';

import { toast } from 'sonner';
import { useCallback } from 'react';
import { CheckCircle2Icon, TriangleAlert } from 'lucide-react';
import { useAcceptTransfer, useCancelTransfer, useRejectTransfer } from '@konfigyr/hooks';
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
import {
  AcceptTransferDescription,
  AcceptTransferLabel,
  AcceptTransferTitle,
  CancelTransferDescription,
  CancelTransferLabel,
  CancelTransferTitle,
  RejectTransferDescription,
  RejectTransferLabel,
  RejectTransferTitle,
  TransferAcceptedSuccessMessage,
  TransferCanceledSuccessMessage,
  TransferRejectedSuccessMessage,
} from '@konfigyr/components/artifactory/transfers/messages';
import type { AriaRole, ReactElement, ReactNode } from 'react';
import type { ArtifactOwnershipTransfer } from '@konfigyr/hooks/types';

type TransferActionButtonProps = {
  namespace: string;
  transfer: ArtifactOwnershipTransfer;
  children: ReactElement;
  role?: AriaRole;
  nativeButton?: boolean;
  onOpenChange?: (open: boolean) => void;
};

type ConfirmTransferDialogProps = {
  children: ReactElement;
  role?: AriaRole;
  nativeButton?: boolean;
  onOpenChange?: (open: boolean) => void;
  icon: ReactNode;
  title: ReactNode;
  description: ReactNode;
  submitLabel: ReactNode;
  variant: 'default' | 'destructive';
  isPending: boolean;
  onConfirm: () => void;
};

function ConfirmTransferDialog ({
  children,
  role,
  nativeButton,
  onOpenChange,
  icon,
  title,
  description,
  submitLabel,
  variant,
  isPending,
  onConfirm,
}: ConfirmTransferDialogProps) {
  return (
    <AlertDialog onOpenChange={onOpenChange}>
      <AlertDialogTrigger
        nativeButton={nativeButton}
        role={role}
        render={children}
      />
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle className="flex items-center gap-2">
            {icon}
            {title}
          </AlertDialogTitle>
          <AlertDialogDescription>
            {description}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>
            <CancelLabel/>
          </AlertDialogCancel>
          <AlertDialogAction
            variant={variant}
            disabled={isPending}
            loading={isPending}
            onClick={onConfirm}
          >
            {submitLabel}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}

export function AcceptTransferButton ({ namespace, transfer, children, role, nativeButton, onOpenChange }: TransferActionButtonProps) {
  const errorNotification = useErrorNotification();
  const { isPending, mutateAsync: acceptTransfer } = useAcceptTransfer(namespace);

  const onConfirm = useCallback(async () => {
    try {
      await acceptTransfer(transfer.id);
    } catch (error) {
      return errorNotification(error);
    }

    toast.success(<TransferAcceptedSuccessMessage groupId={transfer.groupId}/>);
  }, [transfer, errorNotification, acceptTransfer]);

  return (
    <ConfirmTransferDialog
      role={role}
      nativeButton={nativeButton}
      onOpenChange={onOpenChange}
      icon={<CheckCircle2Icon className="h-5 w-5 text-success shrink-0"/>}
      title={<AcceptTransferTitle/>}
      description={<AcceptTransferDescription groupId={transfer.groupId} to={transfer.to.slug}/>}
      submitLabel={<AcceptTransferLabel/>}
      variant="default"
      isPending={isPending}
      onConfirm={onConfirm}
    >
      {children}
    </ConfirmTransferDialog>
  );
}

export function RejectTransferButton ({ namespace, transfer, children, role, nativeButton, onOpenChange }: TransferActionButtonProps) {
  const errorNotification = useErrorNotification();
  const { isPending, mutateAsync: rejectTransfer } = useRejectTransfer(namespace);

  const onConfirm = useCallback(async () => {
    try {
      await rejectTransfer(transfer.id);
    } catch (error) {
      return errorNotification(error);
    }

    toast.success(<TransferRejectedSuccessMessage groupId={transfer.groupId}/>);
  }, [transfer, errorNotification, rejectTransfer]);

  return (
    <ConfirmTransferDialog
      role={role}
      nativeButton={nativeButton}
      onOpenChange={onOpenChange}
      icon={<TriangleAlert className="h-5 w-5 text-destructive shrink-0"/>}
      title={<RejectTransferTitle/>}
      description={<RejectTransferDescription groupId={transfer.groupId} to={transfer.to.slug}/>}
      submitLabel={<RejectTransferLabel/>}
      variant="destructive"
      isPending={isPending}
      onConfirm={onConfirm}
    >
      {children}
    </ConfirmTransferDialog>
  );
}

export function CancelTransferButton ({ namespace, transfer, children, role, nativeButton, onOpenChange }: TransferActionButtonProps) {
  const errorNotification = useErrorNotification();
  const { isPending, mutateAsync: cancelTransfer } = useCancelTransfer(namespace);

  const onConfirm = useCallback(async () => {
    try {
      await cancelTransfer(transfer.id);
    } catch (error) {
      return errorNotification(error);
    }

    toast.success(<TransferCanceledSuccessMessage groupId={transfer.groupId}/>);
  }, [transfer, errorNotification, cancelTransfer]);

  return (
    <ConfirmTransferDialog
      role={role}
      nativeButton={nativeButton}
      onOpenChange={onOpenChange}
      icon={<TriangleAlert className="h-5 w-5 text-destructive shrink-0"/>}
      title={<CancelTransferTitle/>}
      description={<CancelTransferDescription groupId={transfer.groupId} from={transfer.from.slug}/>}
      submitLabel={<CancelTransferLabel/>}
      variant="destructive"
      isPending={isPending}
      onConfirm={onConfirm}
    >
      {children}
    </ConfirmTransferDialog>
  );
}
