import { HttpResponse, http } from 'msw';
import {
  isValidService,
  namespaces,
  services,
} from '../mocks';

import type { Artifact, ServiceCatalogProperty } from '@konfigyr/hooks/types';

const list = http.get('http://localhost/api/namespaces/:namespace/services', ({ params }) => {
  const { namespace } = params;

  if (namespace === namespaces.unknown.slug) {
    return HttpResponse.json({
      status: 404,
      title: 'Namespace not found',
      detail: `Namespace with identifier '${namespace}' not found.`,
    }, { status: 404 });
  }

  const data = namespace === namespaces.konfigyr.slug ? Object.values(services) : [];

  return HttpResponse.json({ data });
});

const get = http.get('http://localhost/api/namespaces/:namespace/services/:service', ({ params }) => {
  const { namespace, service } = params;

  if (namespace !== namespaces.konfigyr.slug) {
    return HttpResponse.json({
      status: 404,
      title: 'Not found',
      detail: `Namespace with identifier '${namespace}' not found.`,
    });
  }

  switch (service) {
    case services.konfigyrApi.slug:
      return HttpResponse.json(services.konfigyrApi);
    case services.konfigyrId.slug:
      return HttpResponse.json(services.konfigyrId);
    default:
      return HttpResponse.json({
        status: 404,
        title: 'Not found',
        detail: `Service with identifier '${service}' not found.`,
      }, { status: 404 });
  }
});

const create = http.post('http://localhost/api/namespaces/:namespace/services', async ({ params, request }) => {
  const { namespace } = params;

  if (namespace === namespaces.unknown.slug) {
    return HttpResponse.json({
      status: 404,
      title: 'Namespace not found',
      detail: `Namespace with identifier '${namespace}' not found.`,
    }, { status: 404 });
  }

  const body = await request.clone().json() as Record<string, unknown>;

  if (body.slug === 'invalid-service-identifier') {
    return HttpResponse.json({
      status: 400,
      title: 'Can not create service',
      detail: 'Service identifier is invalid.',
    }, { status: 400 });
  }

  return HttpResponse.json({
    id: body.slug,
    namespace: namespaces.konfigyr.id,
    ...body,
  }, { status: 201 });
});

const update = http.put('http://localhost/api/namespaces/:namespace/services/:service', async ({ params, request }) => {
  const { namespace } = params;

  if (namespace === namespaces.unknown.slug) {
    return HttpResponse.json({
      status: 404,
      title: 'Namespace not found',
      detail: `Namespace with identifier '${namespace}' not found.`,
    }, { status: 404 });
  }

  const body = await request.clone().json() as Record<string, unknown>;

  if (body.name === 'invalid service name') {
    return HttpResponse.json({
      status: 400,
      title: 'Bad request',
      detail: `Invalid service name: ${body.name}.`,
      errors: [{
        detail: 'Invalid service name.',
        pointer: 'name',
      }],
    }, { status: 400 });
  }

  return HttpResponse.json({
    id: body.slug,
    namespace: namespaces.konfigyr.id,
    ...body,
  }, { status: 201 });
});

const manifest = http.get('http://localhost/api/namespaces/:namespace/services/:service/manifest', ({ params }) => {
  const { namespace, service } = params;

  if (namespace !== namespaces.konfigyr.slug) {
    return HttpResponse.json({
      status: 404,
      title: 'Not found',
      detail: `Namespace with identifier '${namespace}' not found.`,
    });
  }

  if (!isValidService(service)) {
    return HttpResponse.json({
      status: 404,
      title: 'Not found',
      detail: `Service with identifier '${service}' not found.`,
    }, { status: 404 });
  }

  let artifacts: Array<Artifact> = [];

  // only konfigyr-api service should have artifacts...
  if (service === services.konfigyrApi.slug) {
    artifacts = [{
      id: 'com.konfigyr:konfigyr-crypto-jdbc:1.0.0-RC5',
      groupId: 'com.konfigyr',
      artifactId: 'konfigyr-crypto-jdbc',
      version: '1.0.0-RC5',
    }, {
      id: 'org.springframework.boot:spring-boot:4.0.3',
      groupId: 'org.springframework.boot',
      artifactId: 'spring-boot',
      version: '4.0.3',
    }, {
      id: 'org.springframework.boot:spring-boot-autoconfigure:4.0.3',
      groupId: 'org.springframework.boot',
      artifactId: 'spring-boot-autoconfigure',
      version: '4.0.3',
    }];
  }

  return HttpResponse.json({
    id: `${service}-manifest`,
    name: `Manifest for: ${service}`,
    artifacts,
  });
});

const catalog = http.get('http://localhost/api/namespaces/:namespace/services/:service/catalog', ({ params }) => {
  const { namespace, service } = params;

  if (namespace !== namespaces.konfigyr.slug) {
    return HttpResponse.json({
      status: 404,
      title: 'Not found',
      detail: `Namespace with identifier '${namespace}' not found.`,
    });
  }

  if (!isValidService(service)) {
    return HttpResponse.json({
      status: 404,
      title: 'Not found',
      detail: `Service with identifier '${service}' not found.`,
    }, { status: 404 });
  }

  let properties: Array<ServiceCatalogProperty> = [];

  // only konfigyr-api service should have artifacts...
  if (service === services.konfigyrApi.slug) {
    properties = [{
      artifact: 'org.springframework.boot:spring-boot-autoconfigure:4.0.3',
      name:	'spring.aop.auto',
      schema:	{ type: 'boolean' },
      typeName:	'java.lang.Boolean',
      description: 'Add @EnableAspectJAutoProxy.',
      defaultValue: 'true',
    }, {
      artifact: 'org.springframework.boot:spring-boot-autoconfigure:4.0.3',
      name:	'spring.aop.proxy-target-class',
      schema:	{ type: 'boolean' },
      typeName:	'java.lang.Boolean',
      description: 'Whether subclass-based (CGLIB) proxies are to be created (true), as opposed to standard Java interface-based proxies (false).',
      defaultValue: 'true',
    }, {
      artifact: 'org.springframework.boot:spring-boot-autoconfigure:4.0.3',
      name:	'spring.web.resources.cache.period',
      schema:	{ type: 'string', format: 'duration' },
      typeName:	'java.time.Duration',
      description: 'Cache period for the resources served by the resource handler. If a duration suffix is not specified, seconds will be used. Can be overridden by the \'spring.web.resources.cache.cachecontrol\' properties.',
    }, {
      artifact: 'org.springframework.boot:spring-boot-autoconfigure:4.0.3',
      name:	'spring.web.resources.chain.strategy.content.paths',
      schema:	{ type: 'array', items: { type: 'string' } },
      typeName:	'java.lang.String[]',
      description: 'List of patterns to apply to the content Version Strategy.',
      defaultValue: '/**',
    }];
  }

  return HttpResponse.json({
    version: 'latest',
    properties,
  });
});

export default [
  list,
  get,
  create,
  update,
  manifest,
  catalog,
];
