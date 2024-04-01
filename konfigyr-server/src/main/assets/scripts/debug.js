/**
 * Performs an assertions upon a state and throws an error with a supplied error message.
 *
 * @param state   state to be asserted
 * @param message error message to be thrown
 */
export const assert = (state, message) => {
    if (process.env.NODE_ENV !== 'production' && state === false) {
        throw new Error(`Assertion error: ${message}`);
    }
};
