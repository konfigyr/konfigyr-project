import { afterEach, describe, expect, test } from 'vitest';
import { cleanup } from '@testing-library/react';
import { KeysetTable } from '@konfigyr/components/kms/keyset-table';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import { kms, namespaces } from '@konfigyr/test/helpers/mocks';

describe('components | kms | <KeysetTable/>', () => {
  afterEach(() => cleanup());

  test('should render keyset table with pending state', () => {
    const result = renderComponentWithRouter((
      <KeysetTable namespace={namespaces.konfigyr} isPending={true} />
    ));

    expect(result.getByRole('table')).toBeInTheDocument();
    expect(result.queryByRole('alert')).not.toBeInTheDocument();
    expect(result.container.querySelector('[data-slot="keyset-loading-skeleton"]')).toBeInTheDocument();
  });

  test('should render keyset table with an error state', () => {
    const result = renderComponentWithRouter((
      <KeysetTable namespace={namespaces.konfigyr} error={new Error('Failed to retrieve keysets')} />
    ));

    expect(result.getByRole('table')).toBeInTheDocument();
    expect(result.getByRole('alert')).toBeInTheDocument();
  });

  test('should render keysets', () => {
    const keysets = [kms.encryptingKeyset, kms.signingKeyset, kms.disabledKeyset, kms.destroyedKeyset];

    const result = renderComponentWithRouter((
      <KeysetTable namespace={namespaces.konfigyr} keysets={keysets} />
    ));

    expect(result.getByRole('table')).toBeInTheDocument();
    expect(result.queryByRole('alert')).not.toBeInTheDocument();

    expect(result.getAllByRole('row')).toHaveLength(5);
  });
});
