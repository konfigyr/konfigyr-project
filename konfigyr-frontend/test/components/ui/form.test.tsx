import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest';
import { z } from 'zod';
import { cleanup, render, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { useForm } from '@konfigyr/components/ui/form';

const schema = z.object({
  name: z.string().min(2, 'Name has to be at least two characters long.'),
  age: z.coerce.number().min(18, 'You have to be at least 18 years old to use this service.'),
  description: z.string(),
  enabled: z.boolean(),
});

function ExampleForm({ onSubmit }: { onSubmit: (value: z.infer<typeof schema>) => void }) {
  const form = useForm({
    defaultValues: {
      name: '',
      age: 0,
      description: '',
      enabled: true,
    },
    validators: {
      onSubmit: schema,
    },
    onSubmit: ({ value }) => {
      onSubmit(value);
    },
  });

  return (
    <form.AppForm>
      <form className="grid gap-6" onSubmit={(event) => {
        event.preventDefault();
        event.stopPropagation();
        return form.handleSubmit(event);
      }}>
        <form.AppField name="name" children={(field) => (
          <field.Control
            label="Your name"
            description="Enter your first and last name"
          >
            <field.Input />
          </field.Control>
        )} />

        <form.AppField name="age" children={(field) => (
          <field.Control
            label="Your age"
            description="How old are you?"
          >
            <field.Input type="number" />
          </field.Control>
        )} />

        <form.AppField name="description" children={(field) => (
          <field.Control label="Tell us more about yourself">
            <field.Textarea />
          </field.Control>
        )} />

        <form.AppField name="enabled" children={(field) => (
          <field.Control label="Do you wanna receive newsletters?">
            <field.Switch />
          </field.Control>
        )} />

        <form.Submit>Submit</form.Submit>
      </form>
    </form.AppForm>
  );
}

describe('components | UI | <Input/>', () => {
  beforeEach(() => {
    // Mock the ResizeObserver required by the form components
    vi.stubGlobal('ResizeObserver', class {
      observe() {}
      unobserve() {}
      disconnect() {}
    });
  });

  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
  });

  test('should render form', async () => {
    const onSubmit = vi.fn();

    const { getByRole } = render(<ExampleForm onSubmit={onSubmit}/>);

    await userEvents.type(
      getByRole('textbox', { name: 'Your name' }),
      'John Doe',
    );

    await userEvents.type(
      getByRole('spinbutton', { name: 'Your age' }),
      '22',
    );

    await userEvents.type(
      getByRole('textbox', { name: 'Tell us more about yourself' }),
      'Test enthusiast.',
    );

    await userEvents.click(
      getByRole('switch', { name: 'Do you wanna receive newsletters?' }),
    );

    await userEvents.click(
      getByRole('button', { name: 'Submit' }),
    );

    await waitFor(() => expect(onSubmit).toHaveBeenCalledExactlyOnceWith({
      name: 'John Doe',
      age: '22',
      description: 'Test enthusiast.',
      enabled: false,
    }));
  });

  test('should render form validation errors', async () => {
    const onSubmit = vi.fn();

    const { getByRole } = render(<ExampleForm onSubmit={onSubmit}/>);

    await userEvents.click(
      getByRole('button', { name: 'Submit' }),
    );

    expect(onSubmit).not.toHaveBeenCalled();

    await waitFor(() => {
      expect(getByRole('textbox', { name: 'Your name' })).toBeInvalid();
      expect(getByRole('textbox', { name: 'Your name' })).toHaveAccessibleDescription(
        'Enter your first and last name Name has to be at least two characters long.',
      );
    });

    await waitFor(() => {
      expect(getByRole('spinbutton', { name: 'Your age' })).toBeInvalid();
      expect(getByRole('spinbutton', { name: 'Your age' })).toHaveAccessibleDescription(
        'How old are you? You have to be at least 18 years old to use this service.',
      );
    });

    await waitFor(() => {
      expect(getByRole('textbox', { name: 'Tell us more about yourself' })).toBeValid();
    });

    await waitFor(() => {
      expect(getByRole('switch')).toBeValid();
    });
  });
});
