import {
    announceFromElement,
    announce as announceFromMessage,
} from '@primer/live-region-element';

/**
 * Announce message update to screen reader.
 *
 * @param {HTMLElement|String} message message or element contents to be announced
 * @param {Object} options announcer options
 */
export function announce(message, options = {}) {
    const { assertive = false, delay = null, appendTo = null } = options || {};
    let from = options.from || null;
    let announcer;

    if (message instanceof HTMLElement) {
        announcer = announceFromElement;
        from = from || message;
    }

    if (typeof message === 'string') {
        announcer = announceFromMessage;
    }

    if (typeof announcer === 'function') {
        announcer.call(null, message, {
            politeness: assertive ? 'assertive' : 'polite',
            delayMs: delay,
            appendTo,
            from,
        });
    }
}
