import { getByTestId, waitFor } from '@testing-library/dom';
import '../../scripts/forms/submit-button';

const render = () => {
    const container = document.createElement('div');
    container.innerHTML = `
        <form>
            <input type="text" name="name" value="Test value" data-testid="input" required>
            <button type="submit" data-testid="button" disabled data-disable-invalid>Submit</button>
        </form>
    `;

    return container;
};

const fillIn = (input, value) => {
    input.value = value;
    return input.form.dispatchEvent(new InputEvent('change'));
};

describe('forms', () => {
    describe('submit-button', () => {
        let container;

        beforeEach(() => {
            container = render();
            document.body.appendChild(container);
        });

        afterEach(() => {
            document.body.removeChild(container);
        });

        it('should update button state to enabled when observed', async () => {
            const button = getByTestId(container, 'button');

            await waitFor(() => expect(button).toBeEnabled());
        });

        it('should register form change event listener to update button state', async () => {
            const input = getByTestId(container, 'input');
            const button = getByTestId(container, 'button');

            fillIn(input, '');

            await waitFor(() => expect(button).toBeDisabled());

            fillIn(input, 'Value');

            await waitFor(() => expect(button).toBeEnabled());
        });

        it('should unregister form change event listener when button is removed', async () => {
            const input = getByTestId(container, 'input');
            const button = getByTestId(container, 'button');

            fillIn(input, '');

            await waitFor(() => expect(button).toBeDisabled());

            button.remove();

            await waitFor(() => expect(button).toBeEnabled());
        });

        it('should not register listeners for for the same form', async () => {
            const form = document.querySelector('form');

            let invoked = false;
            form.addEventListener = () => {
                invoked = true;
            };

            const button = document.createElement('button');
            button.disabled = true;
            button.setAttribute('data-disable-invalid', 'true');
            form.append(button);

            await waitFor(() => expect(invoked).toBe(false));
        });

        it('should not register listeners for buttons outside of the form', async () => {
            const element = document.createElement('div');
            element.innerHTML = `
                <button type="submit" data-testid="standalone-button" disabled data-disable-invalid>Submit</button>
            `;

            document.body.appendChild(element);

            const button = getByTestId(element, 'standalone-button');

            await waitFor(() => expect(button).toBeDisabled());
        });
    });
});
