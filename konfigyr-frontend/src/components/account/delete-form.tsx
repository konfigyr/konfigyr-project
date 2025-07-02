'use client';

import type { AccountAction } from 'konfigyr/app/settings/actions';

import { useActionState, useState } from 'react';
import { useTranslations } from 'next-intl';
import {
  Button,
  Card,
  CardAction,
  CardContent,
  CardFooter,
  CardHeader,
  Modal,
  ModalHeader,
  ModalTitle,
  ModalContent,
  ModalFooter,
  ModalCancel,
  ModalDescription,
} from 'konfigyr/components/ui';

function ConfirmationDialog({
  open,
  onOpenChange,
  onSubmit,
}: {
  open: boolean,
  onOpenChange: (state: boolean) => void,
  onSubmit: AccountAction,
}) {
  const t = useTranslations('account.form.delete.confirmation');
  const [state, action, isPending] = useActionState(onSubmit, {});

  return (
    <Modal open={open} onOpenChange={onOpenChange}>
      <ModalContent>
        <ModalHeader>
          <ModalTitle>
            {t('title')}
          </ModalTitle>
        </ModalHeader>
        <ModalDescription>
          {t('description')}
        </ModalDescription>

        {state?.error && (
          <div className="my-2 text-sm text-destructive">
            <p className="mb-1 font-medium">{state.error.title}</p>
            <p>{state.error.detail}</p>
          </div>
        )}

        <ModalFooter>
          <ModalCancel>
            {t('cancel')}
          </ModalCancel>
          <form action={action}>
            <Button loading={isPending} type="submit">
              {t('action')}
            </Button>
          </form>
        </ModalFooter>
      </ModalContent>
    </Modal>
  );
}

export function AccountDeleteForm({ action }: { action: AccountAction }) {
  const t = useTranslations('account.form.delete');
  const [open, setOpen] = useState(false);

  return (
    <>
      <form action={() => setOpen(true)}>
        <Card className="my-8">
          <CardHeader title={t('label')}/>
          <CardContent>
            <p>{t('help')}</p>
          </CardContent>
          <CardFooter className="justify-end border-t">
            <CardAction>
              <Button variant="destructive" type="submit">
                {t('submit')}
              </Button>
            </CardAction>
          </CardFooter>
        </Card>
      </form>

      <ConfirmationDialog
        open={open}
        onOpenChange={setOpen}
        onSubmit={action}
      />
    </>
  );
}
