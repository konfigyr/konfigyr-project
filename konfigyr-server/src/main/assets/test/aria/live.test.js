import { getByTestId, waitFor } from '@testing-library/dom';
import '@testing-library/jest-dom';
import { announce } from '../../scripts/aria/live';

const render = () => {
    const container = document.createElement('div');
    container.innerHTML = `
        <live-region data-testid="live-region">
        <button data-testid="announce">Announced message</button>
    `;

    return container;
};

describe('aria', () => {
    describe('announce', () => {
        let container;

        beforeEach(() => {
            container = render();
            document.body.appendChild(container);
        });

        afterEach(() => {
            document.body.removeChild(container);
        });

        it('should announce polite message', async () => {
            const region = getByTestId(container, 'live-region');
            expect(region?.shadowRoot).toBeTruthy();

            expect(() => announce('Announced message')).not.toThrow();

            await waitFor(() =>
                expect(region.getMessage('polite')).toBe('Announced message'),
            );
            await waitFor(() =>
                expect(region.getMessage('assertive')).toBeFalsy(),
            );
        });

        it('should announce assertive message', async () => {
            const region = getByTestId(container, 'live-region');
            expect(region?.shadowRoot).toBeTruthy();

            expect(() => {
                announce('Announced message', {
                    assertive: true,
                });
            }).not.toThrow();

            await waitFor(() =>
                expect(region.getMessage('polite')).toBeFalsy(),
            );
            await waitFor(() =>
                expect(region.getMessage('assertive')).toBe(
                    'Announced message',
                ),
            );
        });

        it('should announce element polite contents', async () => {
            const region = getByTestId(container, 'live-region');
            expect(region?.shadowRoot).toBeTruthy();

            const button = getByTestId(container, 'announce');

            expect(() => announce(button)).not.toThrow();

            await waitFor(() =>
                expect(region.getMessage('polite')).toBe('Announced message'),
            );
            await waitFor(() =>
                expect(region.getMessage('assertive')).toBeFalsy(),
            );
        });

        it('should announce element assertive contents', async () => {
            const region = getByTestId(container, 'live-region');
            expect(region?.shadowRoot).toBeTruthy();

            const button = getByTestId(container, 'announce');

            expect(() => {
                announce(button, {
                    assertive: true,
                });
            }).not.toThrow();

            await waitFor(() =>
                expect(region.getMessage('polite')).toBeFalsy(),
            );
            await waitFor(() =>
                expect(region.getMessage('assertive')).toBe(
                    'Announced message',
                ),
            );
        });

        it('should not announce for invalid arguments', async () => {
            const region = getByTestId(container, 'live-region');
            expect(region?.shadowRoot).toBeTruthy();

            expect(() => announce(12562)).not.toThrow();

            await waitFor(() =>
                expect(region.getMessage('polite')).toBeFalsy(),
            );
            await waitFor(() =>
                expect(region.getMessage('assertive')).toBeFalsy(),
            );
        });
    });
});
