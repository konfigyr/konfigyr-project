import observe from 'spect';
import { validate } from './submit-button.js';
import { announce } from '../aria/live';

const helpTexts = new WeakMap();

/**
 * Creates the `.help-text` HTML element that would contain the success or info text
 * obtained from the server check endpoint.
 *
 * @param text help text to be shown by the element
 * @param id unique element identifier
 * @return {HTMLDivElement} help text element
 */
const createHelpElement = (text, id) => {
    const element = document.createElement('small');
    element.id = id;
    element.classList.add('help-text', 'success');
    element.innerHTML = text;

    return element;
};

/**
 * Creates the `.validation-error` HTML element that would contain the error text
 * obtained from the server check endpoint.
 *
 * @param text error text to be shown by the element
 * @param id unique element identifier
 * @return {HTMLDivElement} validation error element
 */
const createValidationErrorElement = (text, id) => {
    const element = document.createElement('div');
    element.id = id;
    element.classList.add('validation-error', 'error');
    element.innerHTML = text;

    return element;
};

/**
 * Reset the input and its container by removing any generated `.success` or `.error`
 * elements and resetting the original contents of the `.help-text`.
 *
 * @param input
 * @param container
 */
const reset = (input, container) => {
    input.classList.remove('is-loading');

    const help = container.querySelector('.help-text');

    // attempt to replace the help text node with its original
    // contents if there were any in the first place
    if (help) {
        const content = helpTexts.get(help);

        if (content) {
            help.innerHTML = content;
        }
    }

    // remove any generated success or error elements
    container.querySelector('.success')?.remove();
    container.querySelector('.error')?.remove();
};

/**
 * Registers a selector observer for the `auto-check` custom element.
 * Once the button is detected, we need to register event listeners that can
 * append results that were obtained from the server check endpoint.
 */
observe('auto-check', el => {
    const input = el.querySelector('input');

    if (!input) {
        return;
    }

    const container = input.closest('.form-group') || el;
    const form = input.form;

    let id = null;

    // generates the unique element identifier that would be used by the input response
    // check elements that we are going to create if the help text is not present
    const generateId = () => {
        if (!id) {
            id = `${input.id}-checks`;
        }
        return id;
    };

    // retrieve the original `aria-describedby` attribute value so that we
    // can append it when the user focuses out if the input field as during
    // updates we need to remove because we are using the `aria-live` announcer
    // to notify the user about the changes in the input from the server
    const ariaDescribedby = input.getAttribute('aria-describedby');

    // check if there is a help text associated with the element
    // and add it to the cache in order reset their contents
    // when they are replaced by the auto-check response
    const help = container.querySelector('.help-text');
    if (help) {
        if (!help.id) {
            help.id = `${input.id}-help`;
        }
        helpTexts.set(help, help.innerHTML);
    }

    el.addEventListener('loadstart', () => {
        reset(input, container);
        container.classList.add('is-loading');
    });

    el.addEventListener('loadend', () => {
        container.classList.remove('is-loading');
    });

    input.addEventListener('focusout', () => {
        input.setAttribute(
            'aria-describedby',
            [id, ariaDescribedby].filter(Boolean).join(' '),
        );
    });

    input.addEventListener('input', () => {
        input.removeAttribute('aria-describedby');
        if (!input.value) {
            reset(input, container);
        }
    });

    input.addEventListener('auto-check-success', async event => {
        validate(form);

        if (!event.response) {
            return;
        }

        const text = await event.response.text();

        if (help instanceof HTMLElement) {
            help.innerHTML = text;
            announce(help, { from: container });
        } else {
            const element = createHelpElement(text, generateId());
            container.append(element);
            announce(element, { from: container });
        }
    });

    input.addEventListener('auto-check-error', async event => {
        validate(form);

        if (!event.response) {
            return;
        }

        const text = await event.response.text();

        if (help instanceof HTMLElement) {
            help.innerHTML = text;
            announce(help, { from: container });
        } else {
            const element = createValidationErrorElement(text, generateId());
            container.append(element);
            announce(element, { from: container });
        }
    });
});
