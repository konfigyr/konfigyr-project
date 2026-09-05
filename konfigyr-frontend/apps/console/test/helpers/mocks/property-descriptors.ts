import type { PropertyDescriptor } from '@konfigyr/hooks/artifactory/types';

type PropertyDescriptorMock = PropertyDescriptor & { artifact: string };

export const springAopProperties: Array<PropertyDescriptorMock> = [{
  artifact: 'org.springframework.boot:spring-boot-autoconfigure:4.0.3',
  name: 'spring.aop.auto',
  schema: { type: 'boolean' },
  typeName: 'java.lang.Boolean',
  description: 'Add @EnableAspectJAutoProxy.',
  defaultValue: 'true',
}, {
  artifact: 'org.springframework.boot:spring-boot-autoconfigure:4.0.3',
  name: 'spring.aop.proxy-target-class',
  schema: { type: 'boolean' },
  typeName: 'java.lang.Boolean',
  description: 'Whether subclass-based (CGLIB) proxies are to be created (true), as opposed to standard Java interface-based proxies (false).',
  defaultValue: 'true',
}];

export const springConfigProperties: Array<PropertyDescriptorMock> = [{
  artifact: 'org.springframework.boot:spring-boot:4.0.3',
  name: 'spring.config.location',
  typeName: 'java.lang.String',
  schema: { type: 'string' },
  description: 'Config file locations that replace the defaults.',
}, {
  artifact: 'org.springframework.boot:spring-boot:4.0.3',
  name: 'spring.config.name',
  typeName: 'java.lang.String',
  schema: { type: 'string' },
  description: 'Config file name.',
  defaultValue: 'application',
}];

export const springLoggingProperties: Array<PropertyDescriptorMock> = [{
  artifact: 'org.springframework.boot:spring-boot:4.0.3',
  name: 'logging.config',
  typeName: 'java.lang.String',
  schema: { type: 'string' },
  description: 'Location of the logging configuration file. For instance, `classpath:logback.xml` for Logback.',
}, {
  artifact: 'org.springframework.boot:spring-boot:4.0.3',
  name: 'logging.level',
  typeName: 'java.util.Map<java.lang.String,java.lang.String>',
  schema: {
    type: 'object',
    propertyNames: { type: 'string', examples: ['root', 'sql', 'web'] },
    additionalProperties: { type: 'string', examples: ['debug', 'error', 'fatal', 'info', 'off', 'trace', 'warn'] },
  },
  description: 'Log levels severity mapping. For instance, `logging.level.org.springframework=DEBUG`.',
}, {
  artifact: 'org.springframework.boot:spring-boot:4.0.3',
  name: 'logging.file.max-history',
  typeName: 'java.lang.Integer',
  schema: { type: 'integer' },
  description: 'Maximum number of archive log files to keep. Only supported with the default logback setup.',
  defaultValue: '7',
  deprecation: { replacement: 'logging.logback.rollingpolicy.max-history' },
}, {
  artifact: 'org.springframework.boot:spring-boot:4.0.3',
  name: 'logging.file.max-size',
  typeName: 'org.springframework.util.unit.DataSize',
  schema: { type: 'string', format: 'data-size' },
  description: 'Maximum log file size. Only supported with the default logback setup.',
  defaultValue: '10MB',
  deprecation: { replacement: 'logging.logback.rollingpolicy.max-file-size' },
}, {
  artifact: 'org.springframework.boot:spring-boot:4.0.3',
  name: 'logging.logback.rollingpolicy.max-history',
  schema: { type: 'integer', format: 'int32' },
  typeName: 'java.lang.Integer',
  description: 'Maximum number of archive log files to keep.',
  defaultValue: '7',
}, {
  artifact: 'org.springframework.boot:spring-boot:4.0.3',
  name: 'logging.logback.rollingpolicy.max-file-size',
  schema: { type: 'string', format: 'data-size' },
  typeName: 'org.springframework.util.unit.DataSize',
  description: 'Maximum log file size.',
  defaultValue: '10MB',
}];

export const springWebProperties: Array<PropertyDescriptorMock> = [{
  artifact: 'org.springframework.boot:spring-boot-autoconfigure:4.0.3',
  name: 'spring.web.resources.cache.period',
  schema: { type: 'string', format: 'duration' },
  typeName: 'java.time.Duration',
  description: 'Cache period for the resources served by the resource handler. If a duration suffix is not specified, seconds will be used. Can be overridden by the \'spring.web.resources.cache.cachecontrol\' properties.',
}, {
  artifact: 'org.springframework.boot:spring-boot-autoconfigure:4.0.3',
  name: 'spring.web.resources.chain.strategy.content.paths',
  schema: { type: 'array', items: { type: 'string' } },
  typeName: 'java.lang.String[]',
  description: 'List of patterns to apply to the content Version Strategy.',
  defaultValue: '/**',
}];

export const springSecurityProperties: Array<PropertyDescriptorMock> = [{
  artifact: 'org.springframework.boot:spring-boot-security-oauth2-client:4.0.4',
  name: 'spring.security.oauth2.client.provider',
  typeName: 'java.util.Map<java.lang.String,org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties$Provider>',
  schema: {
    type: 'object',
    additionalProperties: {
      properties: {
        tokenUri: { type: 'string' },
        authorizationUri: { type: 'string' },
        userInfoUri: { type: 'string' },
        jwkSetUri: { type: 'string' },
        userInfoAuthenticationMethod: { type: 'string' },
        userNameAttribute: { type: 'string' },
        issuerUri: { type: 'string' },
      },
      type: 'object',
    },
    propertyNames: {
      examples: ['google', 'github'],
      type: 'string',
    },
  },
  description: 'OAuth provider details.',
}, {
  artifact: 'org.springframework.boot:spring-boot-security-oauth2-client:4.0.4',
  name: 'spring.security.oauth2.client.registration',
  typeName: 'java.util.Map<java.lang.String,org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties$Registration>',
  schema: {
    type: 'object',
    additionalProperties: {
      type: 'object',
      properties: {
        provider: { type: 'string' },
        redirectUri: { type: 'string' },
        authorizationGrantType: { type: 'string' },
        clientAuthenticationMethod: { type: 'string' },
        clientSecret: { type: 'string' },
        clientId: { type: 'string' },
        clientName: { type: 'string' },
        scope: {
          items: {
            type: 'object',
          },
          type: 'array',
        },
      },
    },
    propertyNames: { type: 'string' },
  },
  description: 'OAuth client registrations.',
}];
