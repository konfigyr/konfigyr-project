import { getByTestId, fireEvent, waitFor } from '@testing-library/dom';
import '../../scripts/dialog/index';

const render = () => {
    const container = document.createElement('div');
    container.innerHTML = `
        <button data-testid="open-dialog" data-show-dialog="test-dialog">Open dialog</button>
        <button data-testid="close-dialog" data-close-dialog="test-dialog">Close dialog</button>
        
        <button data-testid="disabled-open-dialog" disabled data-show-dialog="test-dialog">Open dialog</button>
        <button data-testid="disabled-close-dialog" aria-disabled="true" data-close-dialog="test-dialog">Close dialog</button>
        
        <button data-testid="open-non-dialog" data-show-dialog="non-dialog">Open dialog</button>
        <button data-testid="close-non-dialog" data-close-dialog="non-dialog">Close dialog</button>
        
        <dialog id="test-dialog" data-testid="dialog" aria-modal="true">
            Dialog
        </dialog>
        
        <div id="non-dialog" data-testid="non-dialog">
            Non dialog
        </div>
    `;

    return container;
};

describe('dialog', () => {
    let container;

    beforeAll(() => {
        HTMLDialogElement.prototype.showModal = vi.fn(function () {
            this.setAttribute('open', true);
            this.open = true;
        });

        HTMLDialogElement.prototype.close = vi.fn(function () {
            this.removeAttribute('open');
            this.open = false;

            fireEvent(this, new CloseEvent('close'));
        });
    });

    beforeEach(() => {
        container = render();
        document.body.appendChild(container);
    });

    afterEach(() => {
        document.body.removeChild(container);
    });

    it('should open dialog', async () => {
        const button = getByTestId(container, 'open-dialog');
        const dialog = getByTestId(container, 'dialog');

        expect(button).toBeVisible();

        expect(dialog.open).toBeFalsy();
        expect(dialog).not.toHaveAttribute('open');

        button.click();

        await waitFor(() => {
            expect(dialog.open).toBeTruthy();
            expect(dialog).toHaveAttribute('open');
            expect(dialog.showModal).toHaveBeenCalledTimes(1);
        });
    });

    it('should not open dialog when button is disabled', async () => {
        const button = getByTestId(container, 'disabled-open-dialog');
        const dialog = getByTestId(container, 'dialog');

        button.click();

        await waitFor(() => {
            expect(dialog.open).toBeFalsy();
        });
    });

    it('should not open dialog when dialog element is invalid', async () => {
        const button = getByTestId(container, 'open-non-dialog');

        button.click();

        await waitFor(() => {
            expect(getByTestId(container, 'dialog').open).toBeFalsy();
            expect(getByTestId(container, 'non-dialog').open).toBeFalsy();
        });
    });

    it('should close dialog', async () => {
        const button = getByTestId(container, 'close-dialog');
        const dialog = getByTestId(container, 'dialog');

        dialog.showModal();

        expect(button).toBeVisible();
        expect(dialog.open).toBeTruthy();
        expect(dialog).toHaveAttribute('open');

        button.click();

        await waitFor(() => {
            expect(dialog.open).toBeFalsy();
            expect(dialog).not.toHaveAttribute('open');
            expect(dialog.close).toHaveBeenCalledTimes(1);
        });
    });

    it('should not close dialog when button is disabled', async () => {
        const button = getByTestId(container, 'disabled-close-dialog');
        const dialog = getByTestId(container, 'dialog');

        dialog.showModal();

        button.click();

        await waitFor(() => {
            expect(dialog.open).toBeTruthy();
        });
    });

    it('should not close dialog when dialog element is invalid', async () => {
        const button = getByTestId(container, 'open-non-dialog');
        const dialog = getByTestId(container, 'dialog');

        dialog.showModal();

        button.click();

        await waitFor(() => {
            expect(dialog.open).toBeTruthy();
            expect(getByTestId(container, 'non-dialog').open).toBeFalsy();
        });
    });

    it('should append and remove animation classes', async () => {
        const opener = getByTestId(container, 'open-dialog');
        const closer = getByTestId(container, 'close-dialog');

        opener.click();

        await waitFor(() => {
            expect(document.documentElement)
                .toHaveClass('modal-is-open')
                .toHaveClass('modal-is-opening');
        });

        await waitFor(() => {
            expect(document.documentElement).not.toHaveClass(
                'modal-is-opening',
            );
        });

        closer.click();

        await waitFor(() => {
            expect(document.documentElement).not.toHaveClass('modal-is-open');
        });
    });
});
