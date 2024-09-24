import observe from 'spect';

/**
 * Function that would validate if the element is a valid non-disabled button HTML element
 * and attempt to resolve the HTML dialog element in the DOM from the given attribute name.
 *
 * @param {HTMLElement|null|undefined} element
 * @param {String} attribute
 * @return {HTMLDialogElement|null} the resolved dialog element or `null`
 */
function lookupDialogElement(element, attribute) {
    // check if it is an actual button and that is not disabled...
    if (
        !(element instanceof HTMLButtonElement) ||
        element.hasAttribute('disabled') ||
        element.getAttribute('aria-disabled') === 'true'
    ) {
        return null;
    }

    const id = element.getAttribute(attribute);

    if (id === null || id === undefined) {
        return null;
    }

    const dialog = document.getElementById(id);

    if (dialog instanceof HTMLDialogElement) {
        return dialog;
    }

    return null;
}

/**
 * Event listener function that is registered on the modal dialog trigger elements on `click` event.
 * It would attempt to find the target dialog and open it.
 *
 * @param {Event} event
 */
function opener(event) {
    const dialog = lookupDialogElement(event.target, 'data-show-dialog');

    if (dialog) {
        const { documentElement: html } = document;

        html.classList.add('modal-is-open', 'modal-is-opening');

        setTimeout(() => html.classList.remove('modal-is-opening'), 300);

        // register close event that would remove the classes that show dialog
        // animations and block the interaction with the rest of the HTML page
        dialog.addEventListener(
            'close',
            () => html.classList.remove('modal-is-open'),
            { once: true },
        );

        dialog.showModal();

        // A buttons default behaviour in some browsers it to send a pointer event
        // If the behaviour is allowed through the dialog will be shown but then
        // quickly hidden as if it were never shown. This prevents that.
        event.preventDefault();
    }
}

/**
 * Event listener function that is registered on the modal dialog trigger elements on `click` event.
 * It would attempt to find the target dialog and close it.
 *
 * @param {Event} event
 */
function closer(event) {
    const dialog = lookupDialogElement(event.target, 'data-close-dialog');

    if (dialog && dialog.open) {
        dialog.close();
    }
}

/**
 * Registers a selector observer for the `[data-show-dialog]` elements that would add a click
 * event listener that would open target HTML dialogs.
 */
observe('[data-show-dialog]', trigger => {
    trigger.addEventListener('click', opener);

    return () => trigger.removeEventListener('click', opener);
});

/**
 * Registers a selector observer for the `[data-close-dialog]` elements that would add a click
 * event listener that would close target HTML dialogs.
 */
observe('[data-close-dialog]', trigger => {
    trigger.addEventListener('click', closer);

    return () => trigger.removeEventListener('click', closer);
});
