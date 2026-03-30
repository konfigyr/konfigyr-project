import * as profiles from './mocks/profile';
import * as services from './mocks/services';

export * as propertyDescriptors from './mocks/property-descriptors';
export * as accounts from './mocks/account';
export * as kms from './mocks/kms';
export * as namespaces from './mocks/namespace';
export * as applications from './mocks/application';

export const isValidProfile = (slug?: string | unknown) => (
  slug === profiles.development.slug || slug === profiles.staging.slug || slug === profiles.deprecated.slug
);

export const isValidService = (slug?: string | unknown) => (
  slug === services.konfigyrApi.slug || slug === services.konfigyrId.slug
);

export { profiles, services };
