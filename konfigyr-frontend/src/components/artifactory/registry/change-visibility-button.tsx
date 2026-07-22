'use client';

import { toast } from 'sonner';
import { useCallback } from 'react';
import { EyeIcon, EyeOffIcon } from 'lucide-react';
import { useChangeArtifactVisibility } from '@konfigyr/hooks';
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
import { Button } from '@konfigyr/components/ui/button';
import { CancelLabel } from '@konfigyr/components/messages';
import {
  ChangeVisibilityTitle,
  MakePrivateDescription,
  MakePrivateLabel,
  MakePublicDescription,
  MakePublicLabel,
  VisibilityChangedSuccessMessage,
} from '@konfigyr/components/artifactory/registry/messages';

import type { ArtifactDefinition } from '@konfigyr/hooks/artifactory/types';

export function ChangeVisibilityButton({ namespace, artifact }: {
  namespace: string;
  artifact: ArtifactDefinition;
}) {
  const errorNotification = useErrorNotification();
  const { isPending, mutateAsync: changeVisibility } = useChangeArtifactVisibility(namespace);

  const nextVisibility = artifact.visibility === 'PUBLIC' ? 'PRIVATE' : 'PUBLIC';
  const coordinates = `${artifact.groupId}:${artifact.artifactId}`;

  const onConfirm = useCallback(async () => {
    try {
      await changeVisibility({
        groupId: artifact.groupId,
        artifactId: artifact.artifactId,
        visibility: nextVisibility,
      });
    } catch (error) {
      return errorNotification(error);
    }

    toast.success(<VisibilityChangedSuccessMessage artifact={coordinates}/>);
  }, [artifact, nextVisibility, coordinates, errorNotification, changeVisibility]);

  return (
    <AlertDialog>
      <AlertDialogTrigger
        render={
          <Button variant="outline">
            {nextVisibility === 'PUBLIC' ? <EyeIcon/> : <EyeOffIcon/>}
            {nextVisibility === 'PUBLIC' ? <MakePublicLabel/> : <MakePrivateLabel/>}
          </Button>
        }
      />
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>
            <ChangeVisibilityTitle/>
          </AlertDialogTitle>
          <AlertDialogDescription>
            {nextVisibility === 'PUBLIC' ? (
              <MakePublicDescription artifact={coordinates}/>
            ) : (
              <MakePrivateDescription artifact={coordinates}/>
            )}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>
            <CancelLabel/>
          </AlertDialogCancel>
          <AlertDialogAction
            disabled={isPending}
            loading={isPending}
            onClick={onConfirm}
          >
            {nextVisibility === 'PUBLIC' ? <MakePublicLabel/> : <MakePrivateLabel/>}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
