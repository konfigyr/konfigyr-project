import { getByTestId, fireEvent, waitFor } from '@testing-library/dom';
import '@testing-library/jest-dom';
import '@github/auto-check-element';
import '../../scripts/forms/auto-check.js';

const render = (help = false) => {
    const container = document.createElement('div');

    if (help) {
        container.innerHTML = `
            <auto-check src="/check" required data-testid="check">
              <input id="input" data-testid="input" aria-describedby="input-help">
              <small id="input-help" class="help-text">Help message</small>
              <input type="hidden" data-csrf value="csrf">
            </auto-check>\`
        `;
    } else {
        container.innerHTML = `
            <auto-check src="/check" required data-testid="check">
              <input id="input" data-testid="input">
              <input type="hidden" data-csrf value="csrf">
            </auto-check>\`
        `;
    }

    return container;
};

const once = (element, eventName) =>
    new Promise(resolve => {
        element.addEventListener(eventName, resolve, { once: true });
    });

const fillIn = (input, value) => {
    input.value = value;

    return fireEvent(input, new InputEvent('input'));
};

describe('forms', () => {
    afterEach(() => {
        fetch.resetMocks();
    });

    describe('auto-check', () => {
        let container;

        beforeEach(() => {
            container = render(false);
            document.body.appendChild(container);
        });

        afterEach(() => {
            document.body.removeChild(container);
        });

        it('should append loading class for load start and end events', async () => {
            const check = getByTestId(container, 'check');

            fireEvent(check, new Event('loadstart'));

            await waitFor(() => expect(check).toHaveClass('is-loading'));

            fireEvent(check, new Event('loadend'));

            await waitFor(() => expect(check).not.toHaveClass('is-loading'));
        });

        it('should fetch data from server and append success message', async () => {
            const check = getByTestId(container, 'check');
            const input = getByTestId(container, 'input');

            fetch.mockResponseOnce('Success message');

            fillIn(input, 'Some value');

            await once(input, 'auto-check-complete');

            await waitFor(() =>
                expect(check.querySelector('.help-text')).toBeInTheDocument(),
            );

            const help = check.querySelector('.help-text');

            expect(help).toHaveClass('success');
            expect(help).toHaveAttribute('id', 'input-checks');
            expect(help).toHaveTextContent('Success message');

            fireEvent(input, new Event('focusout'));

            await waitFor(() =>
                expect(input).toHaveAccessibleDescription('Success message'),
            );

            fillIn(input, '');

            await waitFor(() =>
                expect(input).not.toHaveAccessibleDescription(),
            );

            await waitFor(() =>
                expect(check.querySelector('.help-text')).toBeFalsy(),
            );
        });

        it('should fetch data from server and append error message', async () => {
            const check = getByTestId(container, 'check');
            const input = getByTestId(container, 'input');

            fetch.mockResponseOnce('Error message', { status: 422 });

            fillIn(input, 'Some value');

            await once(input, 'auto-check-complete');

            await waitFor(() =>
                expect(
                    check.querySelector('.validation-error'),
                ).toBeInTheDocument(),
            );

            const error = check.querySelector('.validation-error');

            expect(error).toHaveClass('error');
            expect(error).toHaveAttribute('id', 'input-checks');
            expect(error).toHaveTextContent('Error message');

            fireEvent(input, new Event('focusout'));

            await waitFor(() =>
                expect(input).toHaveAccessibleDescription('Error message'),
            );

            fillIn(input, '');

            await waitFor(() =>
                expect(input).not.toHaveAccessibleDescription(),
            );

            await waitFor(() =>
                expect(check.querySelector('.validation-error')).toBeFalsy(),
            );
        });

        it('should not display checks when there is no response', async () => {
            const input = getByTestId(container, 'input');

            fireEvent(input, new Event('auto-check-success'));

            await waitFor(() =>
                expect(container.querySelector('#input-checks')).toBeFalsy(),
            );

            fireEvent(input, new Event('auto-check-error'));

            await waitFor(() =>
                expect(container.querySelector('#input-checks')).toBeFalsy(),
            );
        });
    });

    describe('auto-check-help', () => {
        let container;

        beforeEach(() => {
            container = render(true);
            document.body.appendChild(container);
        });

        afterEach(() => {
            document.body.removeChild(container);
        });

        it('should update the help text element with success message', async () => {
            const input = getByTestId(container, 'input');
            const help = container.querySelector('.help-text');

            fetch.mockResponseOnce('Remote message');

            fillIn(input, 'Some value');

            await once(input, 'auto-check-complete');

            await waitFor(() =>
                expect(help).toHaveTextContent('Remote message'),
            );

            fillIn(input, '');
            fireEvent(input, new Event('focusout'));

            await waitFor(() =>
                expect(input).toHaveAccessibleDescription('Help message'),
            );

            await waitFor(() => expect(help).toHaveTextContent('Help message'));
        });

        it('should update the help text element with failed message', async () => {
            const input = getByTestId(container, 'input');
            const help = container.querySelector('.help-text');

            fetch.mockResponseOnce('Remote message', { status: 400 });

            fillIn(input, 'Some value');

            await once(input, 'auto-check-complete');

            await waitFor(() =>
                expect(help).toHaveTextContent('Remote message'),
            );

            fillIn(input, '');
            fireEvent(input, new Event('focusout'));

            await waitFor(() =>
                expect(input).toHaveAccessibleDescription('Help message'),
            );

            await waitFor(() => expect(help).toHaveTextContent('Help message'));
        });
    });

    describe('auto-check-empty', () => {
        let container;

        beforeEach(() => {
            container = document.createElement('div');
            container.innerHTML = `
                <auto-check src="/check" class="is-loading" data-testid="check">
                </auto-check>\`
            `;
            document.body.appendChild(container);
        });

        afterEach(() => {
            document.body.removeChild(container);
        });

        it('should not register event listeners without input', async () => {
            const check = getByTestId(container, 'check');

            fireEvent(check, new Event('loadend'));

            await waitFor(() => expect(check).toHaveClass('is-loading'));
        });
    });
});
