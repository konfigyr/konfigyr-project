import {
  LayoutContent,
  LayoutNavbar,
} from '@konfigyr/components/layout';
import { useNamespace } from '@konfigyr/hooks';
import { NamespaceNameForm } from '@konfigyr/components/namespace/form/name';
import { NamespaceDescriptionForm } from '@konfigyr/components/namespace/form/description';
import { NamespaceSlugForm } from '@konfigyr/components/namespace/form/slug';
import { createFileRoute } from '@tanstack/react-router';

export const Route = createFileRoute('/_authenticated/namespace/$namespace/settings')({
  component: RouteComponent,
});

function RouteComponent() {
  const namespace = useNamespace();

  return (
    <LayoutContent>
      <LayoutNavbar title="Settings"/>

      <div className="w-full lg:w-4/5 xl:w-2/3 space-y-6 px-4 mx-auto">
        <NamespaceNameForm namespace={namespace} />
        <NamespaceSlugForm namespace={namespace} />
        <NamespaceDescriptionForm namespace={namespace} />
      </div>
    </LayoutContent>
  );
}
