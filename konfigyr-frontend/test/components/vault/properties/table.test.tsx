import { describe, expect, test, vi } from 'vitest';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { buildConfigurationProperty } from '@konfigyr/test/helpers/factories/property';
import { ConfigurationPropertyState } from '@konfigyr/hooks/vault/types';
import { PropertiesTable } from '@konfigyr/components/vault/properties/table';

describe('components | vault | properties | <PropertiesTable/>', () => {
  test('should render the value cell without an invalid indicator when the property is valid', () => {
    const { container, getByText } = renderWithQueryClient(
      <PropertiesTable
        properties={[buildConfigurationProperty(
          'spring.aop.proxy-target-class',
          ConfigurationPropertyState.UPDATED,
          { type: 'boolean' },
          'java.lang.Boolean',
          'true',
        )]}
        invalidPropertyNames={new Set()}
        onUpdate={vi.fn()}
      />,
    );

    expect(getByText('true')).toBeInTheDocument();
    expect(container.querySelector('.lucide.lucide-circle-alert')).toBeNull();
  });

  test('should render the value cell invalid indicator when the property is invalid', () => {
    const { container, getByText } = renderWithQueryClient(
      <PropertiesTable
        properties={[buildConfigurationProperty(
          'spring.aop.proxy-target-class',
          ConfigurationPropertyState.UPDATED,
          { type: 'boolean' },
          'java.lang.Boolean',
          'truee',
        )]}
        invalidPropertyNames={new Set(['spring.aop.proxy-target-class'])}
        onUpdate={vi.fn()}
      />,
    );

    expect(getByText('true')).toBeInTheDocument();
    expect(container.querySelector('.lucide.lucide-circle-alert')).not.toBeNull();
  });
});
