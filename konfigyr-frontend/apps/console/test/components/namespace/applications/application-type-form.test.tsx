import { afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import { renderComponentWithRouter } from '@konfigyr/test/helpers/router';
import { ApplicationTypeForm } from '@konfigyr/components/namespace/applications/application-type-form';
import userEvents from '@testing-library/user-event/dist/cjs/index.js';

describe('components | namespace | applications | <ApplicationTypeForm/>', () => {
  afterEach(() => cleanup());

  test('should render all three application type options', () => {
    const { getByRole } = renderComponentWithRouter(
      <ApplicationTypeForm onSubmit={vi.fn()} />,
    );

    expect(getByRole('radio', { name: /service account/i })).toBeInTheDocument();
    expect(getByRole('radio', { name: /ai agent/i })).toBeInTheDocument();
    expect(getByRole('radio', { name: /workload identity/i })).toBeInTheDocument();
  });

  test('should pre-select SERVICE_ACCOUNT by default', () => {
    const { getByRole } = renderComponentWithRouter(
      <ApplicationTypeForm onSubmit={vi.fn()} />,
    );

    expect(getByRole('radio', { name: /service account/i })).toBeChecked();
    expect(getByRole('radio', { name: /ai agent/i })).not.toBeChecked();
    expect(getByRole('radio', { name: /workload identity/i })).not.toBeChecked();
  });

  test('should call onSubmit with SERVICE_ACCOUNT when submitting without changing selection', async () => {
    const onSubmit = vi.fn();
    const user = userEvents.setup();
    const { getByRole } = renderComponentWithRouter(
      <ApplicationTypeForm onSubmit={onSubmit} />,
    );

    await user.click(
      getByRole('button', { name: /continue to application configuration/i }),
    );

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith('SERVICE_ACCOUNT');
    });
  });

  test('should call onSubmit with AGENT when AGENT is selected', async () => {
    const onSubmit = vi.fn();
    const user = userEvents.setup();
    const { getByRole } = renderComponentWithRouter(
      <ApplicationTypeForm onSubmit={onSubmit} />,
    );

    await user.click(getByRole('radio', { name: /ai agent/i }));
    await user.click(
      getByRole('button', { name: /continue to application configuration/i }),
    );

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith('AGENT');
    });
  });

  test('should call onSubmit with WORKLOAD when WORKLOAD is selected', async () => {
    const onSubmit = vi.fn();
    const user = userEvents.setup();
    const { getByRole } = renderComponentWithRouter(
      <ApplicationTypeForm onSubmit={onSubmit} />,
    );

    await user.click(getByRole('radio', { name: /workload identity/i }));
    await user.click(
      getByRole('button', { name: /continue to application configuration/i }),
    );

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith('WORKLOAD');
    });
  });

  test('should render a continue button', () => {
    const { getByRole } = renderComponentWithRouter(
      <ApplicationTypeForm onSubmit={vi.fn()} />,
    );

    expect(getByRole('button', { name: /continue to application configuration/i })).toBeInTheDocument();
  });
});
