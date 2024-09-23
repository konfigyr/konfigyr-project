import observe from 'spect';

const DEFAULT_TIMEOUT = 6000;
const timer = Symbol('notification-close-timeout');

/**
 * Registers the close timeout timer and shows the notification element to the user.
 *
 * @param {HTMLElement} notification
 */
function register(notification) {
    const timeout = parseInt(notification.getAttribute('data-timeout')) || DEFAULT_TIMEOUT;

    notification.classList.add('show');

    notification[timer] = setTimeout(() => remove(notification), timeout);
}

/**
 * Removes the timeout callback function as the element is no longer present in the DOM.
 *
 * @param {HTMLElement} notification
 */
function unregister(notification) {
    clearTimeout(notification[timer]);
}

/**
 * Hides the notification element and removes it from the DOM once the exit animation is done.
 *
 * @param {HTMLElement} notification
 */
function remove(notification) {
    notification.classList.add('hide');

    notification.addEventListener('animationend', () => notification.remove(), { once: true });
}

/**
 * Registers a selector observer for the `.notification` elements.
 * Once the notification is detected, the timeout function is added that would remove the element form the DOM.
 */
observe('.notification', notification => {
    register(notification);

    return () => unregister(notification);
});
