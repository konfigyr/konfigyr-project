import { HttpResponse, http } from 'msw';
import {
  ChangeRequestHistoryType,
  ChangeRequestMergeStatus,
  ChangeRequestState,
  PropertyTransitionType,
} from '@konfigyr/hooks/vault/types';
import { konfigyr } from '../mocks/namespace';
import { konfigyrApi, konfigyrId } from '../mocks/services';
import { development, staging } from '../mocks/profile';

const list = http.get('http://localhost/api/namespaces/:namespace/services/:service/changes', ({ params }) => {
  if (params.namespace !== konfigyr.slug) {
    return HttpResponse.json({
      status: 404,
      title: 'Namespace not found',
      detail: `Namespace with slug '${params.namespace}' not found.`,
    }, { status: 404 });
  }

  if (params.service === konfigyrId.slug) {
    return HttpResponse.json({ data: [], metadata: { total: 0 } });
  }

  if (params.service !== konfigyrApi.slug) {
    return HttpResponse.json({
      status: 404,
      title: 'Service not found',
      detail: `Service with slug '${params.service}' not found.`,
    }, { status: 404 });
  }

  return HttpResponse.json({
    data: [{
      id: 'first-change-request',
      service: konfigyrApi,
      profile: development,
      number: 1,
      state: ChangeRequestState.OPEN,
      mergeState: ChangeRequestMergeStatus.MERGEABLE,
      subject: 'Update application name',
      description: {
        html: '<p>Align the application name with the new naming convention</p>',
        markdown: 'Align the application name with the new naming convention',
      },
      createdBy: 'John Doe',
      createdAt: new Date(Date.now() - 1000 * 60 * 12).toISOString(),
      updatedAt: new Date(Date.now() - 1000 * 60 * 7).toISOString(),
    }, {
      id: 'second-change-request',
      service: konfigyrApi,
      profile: staging,
      number: 2,
      state: ChangeRequestState.OPEN,
      mergeState: ChangeRequestMergeStatus.MERGEABLE,
      subject: 'Update datasource URL',
      description: {
        html: '<p>Point to the staging database</p>',
        markdown: 'Point to the staging database',
      },
      createdBy: 'John Doe',
      createdAt: new Date(Date.now() - 1000 * 60 * 7).toISOString(),
      updatedAt: new Date(Date.now() - 1000 * 60 * 2).toISOString(),
    }],
    metadata: { page: 2, size: 5 },
  });
});

const withValidChangerRequest = (
  namespace: string,
  service: string,
  number: string,
  supplier: () => HttpResponse<any>,
) => {
  if (namespace !== konfigyr.slug) {
    return HttpResponse.json({
      status: 404,
      title: 'Namespace not found',
      detail: `Namespace with slug '${namespace}' not found.`,
    }, { status: 404 });
  }

  if (service !== konfigyrApi.slug) {
    return HttpResponse.json({
      status: 404,
      title: 'Service not found',
      detail: `Service with slug '${service}' not found.`,
    }, { status: 404 });
  }

  if (number !== '1') {
    return HttpResponse.json({
      status: 404,
      title: 'Change request not found',
      detail: `Change request with number '${number}' not found.`,
    }, { status: 404 });
  }

  return supplier();
};

const get = http.get('http://localhost/api/namespaces/:namespace/services/:service/changes/:number', ({ params }) => {
  return withValidChangerRequest(params.namespace as string, params.service as string, params.number as string, () => HttpResponse.json({
    id: 'first-change-request',
    service: konfigyrApi,
    profile: development,
    number: 1,
    state: ChangeRequestState.OPEN,
    mergeState: ChangeRequestMergeStatus.MERGEABLE,
    count: 3,
    subject: 'Update application name',
    description: {
      html: '<p>Align the application name with the new naming convention</p>',
      markdown: 'Align the application name with the new naming convention',
    },
    createdBy: 'John Doe',
    createdAt: new Date(Date.now() - 1000 * 60 * 12).toISOString(),
    updatedAt: new Date(Date.now() - 1000 * 60 * 7).toISOString(),
  }));
});

const changes = http.get('http://localhost/api/namespaces/:namespace/services/:service/changes/:number/changes', ({ params }) => {
  return withValidChangerRequest(params.namespace as string, params.service as string, params.number as string, () => HttpResponse.json({
    data: [{
      name: 'logging.file.max-size',
      action: PropertyTransitionType.ADDED,
      to: '10GB',
    }, {
      name: 'logging.level.com.konfigyr.test',
      action: PropertyTransitionType.UPDATED,
      from: 'DEBUG',
      to: 'INFO',
    }, {
      name: 'logging.file.max-history',
      action: PropertyTransitionType.REMOVED,
      from: '10',
    }],
  }));
});

const history = http.get('http://localhost/api/namespaces/:namespace/services/:service/changes/:number/history', ({ params }) => {
  return withValidChangerRequest(params.namespace as string, params.service as string, params.number as string, () => HttpResponse.json({
    data: [{
      id: 'opened-event',
      type: ChangeRequestHistoryType.CREATED,
      initiator: 'Jane Doe',
      timestamp: new Date(Date.now() - 1000 * 60 * 12).toISOString(),
    }, {
      id: 'approved-event',
      type: ChangeRequestHistoryType.APPROVED,
      initiator: 'Jane Doe',
      timestamp: new Date(Date.now() - 1000 * 60 * 8).toISOString(),
    }],
  }));
});

const update = http.put('http://localhost/api/namespaces/:namespace/services/:service/changes/:number', async ({ params, request }) => {
  const { subject, description } = await request.clone().json() as {
    subject?: string,
    description?: string,
  };

  return withValidChangerRequest(params.namespace as string, params.service as string, params.number as string, () => HttpResponse.json({
    id: 'first-change-request',
    service: konfigyrApi,
    profile: development,
    number: 1,
    state: ChangeRequestState.OPEN,
    mergeState: ChangeRequestMergeStatus.MERGEABLE,
    count: 3,
    subject: subject ?? 'Update application name',
    description: {
      html: description ?? '<p>Align the application name with the new naming convention</p>',
      markdown: description ?? 'Align the application name with the new naming convention',
    },
    createdBy: 'John Doe',
    createdAt: new Date(Date.now() - 1000 * 60 * 12).toISOString(),
    updatedAt: new Date().toISOString(),
  }));
});

export default [
  list,
  get,
  changes,
  history,
  update,
];
