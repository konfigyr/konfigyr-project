import { assert } from '../scripts/debug.js';

describe('debug', () => {
    describe('assert', () => {
        it('should assert truthy values', () => {
            expect(() => assert(true, 'Should not thrown error')).not.toThrow();
        });

        it('should throw assertion error', () => {
            expect(() => assert(false, 'assertion failed')).toThrow(
                'Assertion error: assertion failed',
            );
        });
    });
});
