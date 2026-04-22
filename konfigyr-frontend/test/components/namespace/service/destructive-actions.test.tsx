import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client'
import { namespaces, services } from '@konfigyr/test/helpers/mocks'
import { cleanup, RenderResult, waitFor } from '@testing-library/react'
import { ServiceDestructiveActions } from '@konfigyr/components/namespace/service/settings/destructiive-actions'
import userEvents from '@testing-library/user-event/dist/cjs/index.js'

describe('components | namespace | service | <ServiceDestructiveActions/>', () => {
  let result: RenderResult

  const onDelete = vi.fn();

  beforeEach(() => {
    result = renderWithQueryClient((
      <ServiceDestructiveActions namespace={namespaces.konfigyr} service={services.konfigyrId} onDelete={onDelete} />
    ));
  });

  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  test('should render <ServiceDestructiveActions> component', () => {
    expect(result.getByText('Destructive actions')).toBeInTheDocument()

    expect(result.getByText('Delete this service')).toBeInTheDocument()
    expect(result.getByText('This action permanently deletes the service and cannot be undone.')).toBeInTheDocument()
    expect(result.getByText('Delete service')).toBeInTheDocument()
  });

  test('should open the delete confirmation modal', async () => {
    expect(result.getByText('Delete service')).toBeInTheDocument();

    await userEvents.click(
      result.getByRole('button', { name: 'Delete service' }),
    );

    await waitFor(() => {
      expect(
        result.getByText((_, e) =>
          e?.textContent === `Delete ${services.konfigyrId.name} service`
        )
      ).toBeInTheDocument();
    });

    expect(result.getByRole('button', { name: 'Yes, I am sure' })).toBeDisabled();
  });

  test('should open the delete confirmation modal and enable the confirmation button', async () => {
    expect(result.getByText('Delete service')).toBeInTheDocument();

    await userEvents.click(
      result.getByRole('button', { name: 'Delete service' }),
    );

    await userEvents.type(
      result.getByPlaceholderText('Input service name'),
      services.konfigyrId.name,
    );

    expect(result.getByRole('button', { name: 'Yes, I am sure' })).toBeEnabled();
  });

  test('should confirm delete service action', async () => {
    expect(result.getByText('Delete service')).toBeInTheDocument();

    await userEvents.click(
      result.getByRole('button', { name: 'Delete service' }),
    );

    await userEvents.type(
      result.getByPlaceholderText('Input service name'),
      services.konfigyrId.name,
    );

    await userEvents.click(
      result.getByRole('button', { name: 'Yes, I am sure' }),
    );

    expect(onDelete).toHaveBeenCalled();

  });

})
