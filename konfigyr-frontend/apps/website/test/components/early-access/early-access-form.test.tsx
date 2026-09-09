import { afterAll, beforeAll, describe, expect, test, vi } from 'vitest';
import { cleanup, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import { EarlyAccessForm } from '@konfigyr/components/early-access/early-access-form';

import type { RenderResult } from '@testing-library/react';
import type { UserEvent } from '@testing-library/user-event';

describe('components | early-access | <EarlyAccessForm/>', () => {
  let result: RenderResult;
  let user: UserEvent;
  const onSubmit = vi.fn();

  beforeAll(() => {
    user = userEvents.setup();
    result = renderWithMessageProvider(<EarlyAccessForm onSubmit={onSubmit} />);
  });

  afterAll(() => cleanup());

  test('renders every field and the submit button', () => {
    expect(result.getByRole('textbox', { name: 'Name' })).toBeInTheDocument();
    expect(result.getByRole('textbox', { name: 'Work email' })).toBeInTheDocument();
    expect(result.getByRole('textbox', { name: 'Company' })).toBeInTheDocument();
    expect(result.getByRole('combobox', { name: 'Team / org size' })).toBeInTheDocument();
    expect(result.getByRole('textbox', {
      name: 'How many Spring Boot services are you running (roughly)?',
    })).toBeInTheDocument();
    expect(result.getByRole('textbox', {
      name: 'What are you using today for config or secrets, if anything?',
    })).toBeInTheDocument();
    expect(result.getByRole('textbox', { name: 'Anything else you want us to know?' })).toBeInTheDocument();
    expect(result.getByRole('checkbox', { name: /I agree to be contacted/ })).toBeInTheDocument();
    expect(result.getByRole('button', { name: 'Request Early Access' })).toBeInTheDocument();
  });

  test('the honeypot field is present but hidden from real visitors', () => {
    const honeypot = result.container.querySelector('input[name="hp_field"]');

    expect(honeypot).toHaveAttribute('aria-hidden', 'true');
    expect(honeypot).toHaveAttribute('tabindex', '-1');
    expect(honeypot).toHaveAttribute('autocomplete', 'off');
    expect(honeypot?.className).toMatch(/left-\[-9999px\]/);
  });

  test('shows validation errors and blocks submission until required fields and consent are filled in', async () => {
    await user.click(result.getByRole('button', { name: 'Request Early Access' }));

    await waitFor(() => {
      expect(result.getByRole('textbox', { name: 'Name' })).toBeInvalid();
      expect(result.getByRole('textbox', { name: 'Work email' })).toBeInvalid();
      expect(result.getByRole('textbox', { name: 'Company' })).toBeInvalid();
      expect(result.getByRole('combobox', { name: 'Team / org size' })).toBeInvalid();
      expect(result.getByRole('checkbox', { name: /I agree to be contacted/ })).toBeInvalid();
    });

    expect(onSubmit).not.toHaveBeenCalled();
  });

  test('submits the form once every field is valid and consent is checked', async () => {
    onSubmit.mockResolvedValue({ ok: true });

    await user.type(result.getByRole('textbox', { name: 'Name' }), 'Ada Lovelace');
    await user.type(result.getByRole('textbox', { name: 'Work email' }), 'ada@example.com');
    await user.type(result.getByRole('textbox', { name: 'Company' }), 'Analytical Engines Inc.');

    await user.click(result.getByRole('combobox', { name: 'Team / org size' }));

    await waitFor(() => {
      expect(result.getAllByRole('option')).toHaveLength(5);
    });

    await user.click(result.getByRole('option', { name: '11-50' }));

    await user.type(
      result.getByRole('textbox', { name: 'How many Spring Boot services are you running (roughly)?' }),
      '5-10',
    );

    await user.click(result.getByRole('checkbox', { name: /I agree to be contacted/ }));

    await user.click(result.getByRole('button', { name: 'Request Early Access' }));

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({
        name: 'Ada Lovelace',
        email: 'ada@example.com',
        company: 'Analytical Engines Inc.',
        teamSize: '11-50',
        serviceCount: '5-10',
        consent: true,
      }));
    });

    await waitFor(() => {
      expect(result.getByText(/We read every request ourselves/)).toBeInTheDocument();
    });
  });
});

describe('components | early-access | <EarlyAccessForm/> submission failure', () => {
  test('shows the returned error message when submission fails', async () => {
    const user = userEvents.setup();
    const onSubmit = vi.fn().mockResolvedValue({ ok: false, detail: 'Something went wrong submitting your request.' });

    const result = renderWithMessageProvider(<EarlyAccessForm onSubmit={onSubmit} />);

    await user.type(result.getByRole('textbox', { name: 'Name' }), 'Ada Lovelace');
    await user.type(result.getByRole('textbox', { name: 'Work email' }), 'ada@example.com');
    await user.type(result.getByRole('textbox', { name: 'Company' }), 'Analytical Engines Inc.');

    await user.click(result.getByRole('combobox', { name: 'Team / org size' }));

    await waitFor(() => {
      expect(result.getAllByRole('option')).toHaveLength(5);
    });

    await user.click(result.getByRole('option', { name: '11-50' }));

    await user.type(
      result.getByRole('textbox', { name: 'How many Spring Boot services are you running (roughly)?' }),
      '5-10',
    );

    await user.click(result.getByRole('checkbox', { name: /I agree to be contacted/ }));
    await user.click(result.getByRole('button', { name: 'Request Early Access' }));

    await waitFor(() => {
      expect(result.getByText('Something went wrong submitting your request.')).toBeInTheDocument();
    });

    cleanup();
  });
});
