import { useState } from 'react';
import { afterEach, describe, expect, test } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { renderWithQueryClient } from '@konfigyr/test/helpers/query-client';
import { ConfigurationPropertyState } from '@konfigyr/hooks/vault/types';
import { PropertyHistorySidebar } from '@konfigyr/components/vault/properties/history-sidebar';
import { namespaces, profiles, services } from '@konfigyr/test/helpers/mocks';

import type { ConfigurationProperty } from '@konfigyr/hooks/types';

function TestHistorySidebar({ property, opened = false }: { property: ConfigurationProperty<any>, opened?: boolean }) {
  const [open, setOpen] = useState(opened);

  return (
    <PropertyHistorySidebar
      open={open}
      onOpenChange={setOpen}
      property={property}
      namespace={namespaces.konfigyr}
      service={services.konfigyrApi}
      profile={profiles.development}
    />
  );
}

const applicationNameProperty: ConfigurationProperty<string> = {
  name: 'application.name',
  description: 'Application name property',
  typeName: 'java.lang.String',
  state: ConfigurationPropertyState.UNCHANGED,
  value: {
    encoded: 'konfigyr-frontend',
    decoded: 'konfigyr-frontend',
  },
  schema: {
    type: 'string',
  },
};

const applicationProfileProperty: ConfigurationProperty<string> = {
  name: 'application.profile',
  description: 'Application profile property',
  typeName: 'java.lang.String',
  state: ConfigurationPropertyState.UNCHANGED,
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
    expect(result.getByText(applicationNameProperty.value!.encoded)).toBeInTheDocument();
    expect(result.getByText(applicationNameProperty.typeName)).toBeInTheDocument();

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

  test('should display opened sidebar with an empty timeline', async () => {
    const result = renderWithQueryClient(
      <TestHistorySidebar opened={true} property={{
        ...applicationProfileProperty, name: 'empty-configuration-property',
      }} />,
    );

    await waitFor(() => {
      expect(result.getByText('No history found for this property.')).toBeInTheDocument();
    });
  });

  test('should display opened sidebar with timeline', async () => {
    const result = renderWithQueryClient(
      <TestHistorySidebar opened={true} property={applicationProfileProperty} />,
    );

    await waitFor(() => {
      expect(result.getAllByText('Added')).length(1);
      expect(result.getAllByText('Modified')).length(2);
    });
  });
});
