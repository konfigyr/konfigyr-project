import { getByTestId, fireEvent, waitFor } from '@testing-library/dom';
import '../../scripts/notifications/index';

const render = () => {
    const container = document.createElement('div');
    container.innerHTML = `
        <div class="notification" data-testid="notification" data-timeout="600">Notification</div>
    `;

    return container;
};

describe('notifications', () => {
    let container;

    beforeEach(() => {
        container = render();
        document.body.appendChild(container);
    });

    afterEach(() => {
        document.body.removeChild(container);
    });

    it('should remove notification after defined timeout', async () => {
        const notification = getByTestId(container, 'notification');

        expect(notification).toBeInTheDocument();

        await waitFor(() => expect(notification).toHaveClass('show'));

        await waitFor(() => expect(notification).toHaveClass('hide'), {
            timeout: 1000,
        });

        fireEvent(notification, new Event('animationend'));

        await waitFor(() => expect(notification).not.toBeInTheDocument());
    });

    it('should not remove notification animationend event before timeout expires', async () => {
        const notification = getByTestId(container, 'notification');

        fireEvent(notification, new Event('animationend'));

        await waitFor(() => expect(notification).toBeInTheDocument());
    });
});
