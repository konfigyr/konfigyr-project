import { ServiceUpdateForm } from '@konfigyr/components/namespace/service/update-form'
import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { ServiceDestructiveActions } from '@konfigyr/components/namespace/service/settings/destructiive-actions'
import { useCallback } from 'react'

export const Route = createFileRoute(
  '/_authenticated/namespace/$namespace/services/$service/settings',
)({
  component: RouteComponent,
})

function RouteComponent () {
  const { namespace, service } = Route.parentRoute.useLoaderData()

  const navigate = useNavigate()

  const onDelete = useCallback(async () => await navigate({
    to: '/namespace/$namespace',
    params: { namespace: namespace.slug },
  }), [namespace.slug])

  return (
    <div className="w-full lg:w-4/5 xl:w-2/3 space-y-6 px-4 mx-auto">
      <ServiceUpdateForm namespace={namespace} service={service}/>
      <ServiceDestructiveActions namespace={namespace} service={service} onDelete={onDelete}/>
    </div>
  )
}
