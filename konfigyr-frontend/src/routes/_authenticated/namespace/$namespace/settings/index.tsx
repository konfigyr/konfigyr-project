import { useCallback } from 'react';
import {
  LayoutContent,
  LayoutNavbar,
} from '@konfigyr/components/layout';
import { useAccount, useNamespace } from '@konfigyr/hooks';
import { NamespaceDeleteForm } from '@konfigyr/components/namespace/form/delete';
import { NamespaceDescriptionForm } from '@konfigyr/components/namespace/form/description';
import { NamespaceNameForm } from '@konfigyr/components/namespace/form/name';
import { NamespaceSlugForm } from '@konfigyr/components/namespace/form/slug';
import { createServerFn, useServerFn } from '@tanstack/react-start';
import { createFileRoute } from '@tanstack/react-router';
import { DeleteNamespaceSchema, deleteNamespaceHandler } from './-handler';

import type { Namespace } from '@konfigyr/hooks/types';
import type { DeleteNamespaceRequest } from './-handler';

const deleteNamespace = createServerFn({ method: 'POST' })
  .inputValidator(DeleteNamespaceSchema)
  .handler(deleteNamespaceHandler);

const useDeleteNamespace = () => {
  const onDeleteNamespace = useServerFn(deleteNamespace);
  const account = useAccount();

  return useCallback(async (namespace: Namespace) => {
    const data: DeleteNamespaceRequest = {
      namespace: namespace.slug,
    };

    const memberships = account.memberships
      .filter(it => it.namespace !== namespace.slug);

    if (memberships.length > 0) {
      data.redirect = memberships[0].namespace;
    }

    await onDeleteNamespace({ data });
  }, [account, onDeleteNamespace]);
};

export const Route = createFileRoute('/_authenticated/namespace/$namespace/settings/')({
  component: RouteComponent,
});

function RouteComponent() {
  const namespace = useNamespace();
  const onDeleteNamespace = useDeleteNamespace();

  return (
    <LayoutContent>
      <LayoutNavbar title="Settings"/>

      <div className="w-full lg:w-4/5 xl:w-2/3 space-y-6 px-4 mx-auto">
        <NamespaceNameForm namespace={namespace} />
        <NamespaceSlugForm namespace={namespace} />
        <NamespaceDescriptionForm namespace={namespace} />
        <NamespaceDeleteForm namespace={namespace} onDelete={onDeleteNamespace} />
      </div>
    </LayoutContent>
  );
}
