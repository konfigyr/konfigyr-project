import observe from 'spect';

const forms = new WeakMap();

/**
 * Register Form `change` event listener that would check form validation state and update the
 * submit button disabled state.
 *
 * @param {HTMLFormElement} form form element for which the listener should be added
 */
const register = form => {
    if (forms.has(form)) {
        return;
    }

    const listener = () => validate(form);
    form.addEventListener('change', listener);
    forms.set(form, listener);
};

/**
 * Unregister the Form `change` event listener when the button is removed from the DOM.
 *
 * @param {HTMLFormElement} form form element for which the listener should be removed
 */
const unregister = form => {
    const listener = forms.get(form);

    if (listener) {
        form.removeEventListener('change', listener);
        forms.delete(form);
    }
};

/**
 * Validates the form and updates the submit button disabled state.
 *
 * @param form form to be validated
 */
export const validate = form => {
    if (!form) {
        return;
    }

    const validity = form.checkValidity();

    for (const button of form.querySelectorAll(
        'button[data-disable-invalid]',
    )) {
        button.disabled = !validity;
    }
};

/**
 * Registers a selector observer for the Form submit buttons with `data-disable-invalid` attribute.
 * Once the button is detected, the Form `change` event listener is added that would disable or
 * enable the submit button depending on the form validity state.
 */
observe('button[data-disable-invalid]', button => {
    const form = button.form;

    if (form) {
        register(form);
        button.disabled = !form.checkValidity();

        return () => {
            unregister(form);
            button.disabled = false;
        };
    }
});
