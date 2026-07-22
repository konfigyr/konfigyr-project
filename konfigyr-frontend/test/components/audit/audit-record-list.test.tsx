import { afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import { namespaces } from '@konfigyr/test/helpers/mocks';
import { AuditRecordList } from '@konfigyr/components/audit/audit-record-list';

describe('components | audit | <AuditRecordList/>', () => {
  afterEach(() => cleanup());

  test('should render the audit record list items', async () => {
    const { getByRole, getByText } = renderComponentWithRouter(
      <AuditRecordList namespace={namespaces.konfigyr} query={{}} onQueryChange={vi.fn()}/>,
    );

    await waitFor(() => {
      expect(getByText('Namespace was created')).toBeInTheDocument();
    });

    expect(getByRole('listitem', { name: 'Keyset was created' })).toBeInTheDocument();
    expect(getByRole('listitem', { name: 'Service was created' })).toBeInTheDocument();
    expect(getByRole('listitem', { name: 'Profile was created' })).toBeInTheDocument();
    expect(getByRole('listitem', { name: 'Namespace was created' })).toBeInTheDocument();
  });
});
