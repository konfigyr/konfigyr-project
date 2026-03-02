import { afterEach, describe, expect, test, vi } from 'vitest';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import { applications, namespaces } from '@konfigyr/test/helpers/mocks';
import { cleanup, waitFor } from '@testing-library/react';
import {
  ConfirmNamespaceApplicationDeleteAction, ConfirmNamespaceApplicationResetAction,
} from '@konfigyr/components/namespace/applications/confirm-application-action';
import userEvents from '@testing-library/user-event/dist/cjs/index.js';
import * as React from 'react';

describe('components | namespace | applications | confirm application actions', async () => {
  afterEach(() => cleanup());

  test('should render <ConfirmNamespaceApplicationDeleteAction /> component', async () => {
    const { getByText, getByRole } = renderComponentWithRouter(
      <ConfirmNamespaceApplicationDeleteAction
        namespace={namespaces.konfigyr}
        application={applications.konfigyr}
        onConfirm={vi.fn()}
      />,
    );

    await waitFor(() => {
      expect(getByText('Delete application'), 'render delete button').toBeInTheDocument();
    });

    await userEvents.click(
      getByRole('button', { name: /delete application/i }),
    );

    await waitFor(() => {
      expect(getByText('Delete "konfigyr test" application'), 'render title of the confirmation window').toBeInTheDocument();
      expect(getByText('Are you sure you want to delete "konfigyr test" application? This action cannot be undone.'), 'render body of the confirmation window').toBeInTheDocument();

      expect(getByRole('button', { name: 'Cancel' }), 'redner Cancel button').toBeInTheDocument();
      expect(getByRole('button', { name: 'Yes, I am sure' }), 'redner Yes button').toBeInTheDocument();
    });
  });

  test('should render <ConfirmNamespaceApplicationDeleteAction /> component and confirm delete action', async () => {
    const onConfirm = vi.fn();

    const { getByRole, getByText } = renderComponentWithRouter(
      <ConfirmNamespaceApplicationDeleteAction
        namespace={namespaces.konfigyr}
        application={applications.konfigyr}
        onConfirm={onConfirm}
      />,
    );

    await waitFor(() => {
      expect(getByText('Delete application'), 'render delete button').toBeInTheDocument();
    });

    await userEvents.click(
      getByRole('button', { name: /delete application/i }),
    );

    await userEvents.click(
      getByRole('button', { name: 'Yes, I am sure' }),
    );

    expect(onConfirm).toHaveBeenCalled();
  });

  test('should render <ConfirmNamespaceApplicationDeleteAction /> component and cancel delete action', async () => {
    const onConfirm = vi.fn();

    const { getByRole, getByText } = renderComponentWithRouter(
      <ConfirmNamespaceApplicationDeleteAction
        namespace={namespaces.konfigyr}
        application={applications.konfigyr}
        onConfirm={onConfirm}
      />,
    );

    await waitFor(() => {
      expect(getByText('Delete application'), 'render delete button').toBeInTheDocument();
    });

    await userEvents.click(
      getByRole('button', { name: /delete application/i }),
    );

    await userEvents.click(
      getByRole('button', { name: /cancel/i }),
    );

    expect(onConfirm).not.toHaveBeenCalled();
  });

  test('should render <ConfirmNamespaceApplicationResetAction /> component', async () => {
    const { getByText, getByRole } = renderComponentWithRouter(
      <ConfirmNamespaceApplicationResetAction
        namespace={namespaces.konfigyr}
        application={applications.konfigyr}
        onConfirm={vi.fn()}
      />,
    );

    await waitFor(() => {
      expect(getByText('Reset application'), 'render reset button').toBeInTheDocument();
    });

    await userEvents.click(
      getByRole('button', { name: /reset application/i }),
    );

    await waitFor(() => {
      expect(getByText('Reset "konfigyr test" application'), 'render title of the confirmation window').toBeInTheDocument();
      expect(getByText('Are you sure you want to reset "konfigyr test" application? This action cannot be undone.'), 'render body of the confirmation window').toBeInTheDocument();

      expect(getByRole('button', { name: 'Cancel' }), 'redner Cancel button').toBeInTheDocument();
      expect(getByRole('button', { name: 'Yes, I am sure' }), 'redner Yes button').toBeInTheDocument();
    });
  });

  test('should render <ConfirmNamespaceApplicationResetAction /> component and confirm reset action', async () => {
    const onConfirm = vi.fn();

    const { getByRole, getByText } = renderComponentWithRouter(
      <ConfirmNamespaceApplicationResetAction
        namespace={namespaces.konfigyr}
        application={applications.konfigyr}
        onConfirm={onConfirm}
      />,
    );

    await waitFor(() => {
      expect(getByText('Reset application'), 'render reset button').toBeInTheDocument();
    });

    await userEvents.click(
      getByRole('button', { name: /reset application/i }),
    );

    await userEvents.click(
      getByRole('button', { name: 'Yes, I am sure' }),
    );

    expect(onConfirm).toHaveBeenCalled();
  });

  test('should render <ConfirmNamespaceApplicationResetAction /> component and cancel reset action', async () => {
    const onConfirm = vi.fn();

    const { getByRole, getByText } = renderComponentWithRouter(
      <ConfirmNamespaceApplicationResetAction
        namespace={namespaces.konfigyr}
        application={applications.konfigyr}
        onConfirm={onConfirm}
      />,
    );

    await waitFor(() => {
      expect(getByText('Reset application'), 'render reset button').toBeInTheDocument();
    });

    await userEvents.click(
      getByRole('button', { name: /reset application/i }),
    );

    await userEvents.click(
      getByRole('button', { name: /cancel/i }),
    );

    expect(onConfirm).not.toHaveBeenCalled();
  });

});
