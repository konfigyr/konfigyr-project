import { useState } from 'react';
import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { PropertyHistorySidebar } from '@konfigyr/components/vault/properties/history-sidebar';
import { profiles } from '@konfigyr/test/helpers/mocks';

import type { ConfigurationProperty } from '@konfigyr/hooks/types';

function TestHistorySidebar({ property, opened = false }: { property: ConfigurationProperty, opened?: boolean }) {
  const [open, setOpen] = useState(opened);

  return (
    <PropertyHistorySidebar
      open={open}
      onOpenChange={setOpen}
      property={property}
      profile={profiles.staging}
    />
  );
}

const applicationNameProperty: ConfigurationProperty = {
  name: 'application.name',
  description: 'Application name property',
  type: 'java.lang.String',
  state: 'unchanged',
  value: 'konfigyr-frontend',
  schema: {
    type: 'string',
  },
};

const applicationProfileProperty: ConfigurationProperty = {
  name: 'application.profile',
  description: 'Application profile property',
  type: 'java.lang.String',
  state: 'unchanged',
  schema: {
    type: 'string',
    enum: ['staging', 'production'],
  },
};

describe('components | vault | properties | <HistorySidebar/>', () => {
  afterEach(() => cleanup());

  test('should not display opened sidebar when closed', () => {
    const result = renderWithQueryClient(
      <TestHistorySidebar property={applicationNameProperty} />,
    );

    expect(result.queryByRole('dialog', { name: 'Property history' })).toBeNull();
  });

  test('should display opened sidebar and close it', async () => {
    const result = renderWithQueryClient(
      <TestHistorySidebar opened={true} property={applicationNameProperty} />,
    );

    expect(result.getByRole('dialog')).toBeInTheDocument();
    expect(result.getByRole('button', { name: 'Close' })).toBeInTheDocument();

    expect(result.getByText(applicationNameProperty.name)).toBeInTheDocument();
    expect(result.getByText(applicationNameProperty.description!)).toBeInTheDocument();
    expect(result.getByText(applicationNameProperty.value!)).toBeInTheDocument();
    expect(result.getByText(applicationNameProperty.type)).toBeInTheDocument();

    await userEvents.click(result.getByRole('button', { name: 'Close' }));

    await waitFor(() => {
      expect(result.queryByRole('dialog')).toBeNull();
    });
  });

  test('should display opened sidebar with a loading skeleton', async () => {
    const result = renderWithQueryClient(
      <TestHistorySidebar opened={true} property={applicationNameProperty} />,
    );

    await waitFor(() => {
      expect(result.baseElement.querySelector('[data-slot="timeline-item-skeleton"]'))
        .toBeInTheDocument();
    });
  });

  test('should display opened sidebar with timeline', async () => {
    const result = renderWithQueryClient(
      <TestHistorySidebar opened={true} property={applicationProfileProperty} />,
    );

    await waitFor(() => {
      expect(result.getAllByText('added')).length(1);
      expect(result.getAllByText('modified')).length(2);
    });
  });
});
