import { afterEach, describe, expect, test, vi } from 'vitest';
import { cleanup, fireEvent, waitFor } from '@testing-library/react';
import userEvents from '@testing-library/user-event';
import { Toaster } from '@konfigyr/components/ui/sonner';
import { ChangeRequestReviewType } from '@konfigyr/hooks/vault/types';
import { renderWithMessageProvider } from '@konfigyr/test/helpers/messages';
import { ChangeRequestSubmitReview } from '@konfigyr/components/vault/change-request/change-request-submit-review';

describe('components | vault | change-request | <ChangeRequestSubmitReview/>', () => {
  afterEach(() => {
    cleanup();
    vi.resetAllMocks();
  });

  afterEach(() => cleanup());

  test('should render <ChangeRequestSubmitReview/> component', () => {
    const { getByRole } = renderWithMessageProvider(
      <ChangeRequestSubmitReview onReview={vi.fn()} onDiscard={vi.fn()} />,
    );

    expect(getByRole('form')).toBeInTheDocument();
    expect(getByRole('textbox')).toBeInTheDocument();
    expect(getByRole('radiogroup')).toBeInTheDocument();
    expect(getByRole('radio', { name: 'Comment' })).toBeInTheDocument();
    expect(getByRole('radio', { name: 'Approve changes' })).toBeInTheDocument();
    expect(getByRole('radio', { name: 'Request changes' })).toBeInTheDocument();

    expect(getByRole('button', { name: 'Discard change request' })).toBeInTheDocument();
    expect(getByRole('button', { name: 'Discard change request' })).not.toBeDisabled();

    expect(getByRole('button', { name: 'Submit review' })).toBeInTheDocument();
    expect(getByRole('button', { name: 'Submit review' })).toBeDisabled();
  });

  test('should discard the change request', async () => {
    const onDiscard = vi.fn();
    const { getByRole, getByText } = renderWithMessageProvider(
      <>
        <Toaster />
        <ChangeRequestSubmitReview onReview={vi.fn()} onDiscard={onDiscard}/>
      </>,
    );

    fireEvent.click(getByRole('button', { name: 'Discard change request' }));

    expect(onDiscard).toHaveBeenCalledExactlyOnceWith();

    await waitFor(() => {
      expect(getByText('Change request was discarded')).toBeInTheDocument();
    });
  });

  test('should submit change request review without comment', async () => {
    const onReview = vi.fn();
    const { getByRole, getByText } = renderWithMessageProvider(
      <>
        <Toaster />
        <ChangeRequestSubmitReview onReview={onReview} onDiscard={vi.fn()}/>
      </>,
    );

    await userEvents.click(getByRole('radio', { name: 'Approve changes' }));
    await userEvents.click(getByRole('button', { name: 'Submit review' }));

    expect(onReview).toHaveBeenCalledExactlyOnceWith({
      comment: '',
      state: ChangeRequestReviewType.APPROVE,
    });

    await waitFor(() => {
      expect(getByText('Your review was successfully submitted')).toBeInTheDocument();
    });
  });

  test('should submit change request review with comment', async () => {
    const onReview = vi.fn();
    const { getByRole, getByText } = renderWithMessageProvider(
      <>
        <Toaster />
        <ChangeRequestSubmitReview onReview={onReview} onDiscard={vi.fn()}/>
      </>,
    );

    await userEvents.type(
      getByRole('textbox'),
      'This is a test comment',
    );

    await userEvents.click(getByRole('radio', { name: 'Comment' }));
    await userEvents.click(getByRole('button', { name: 'Submit review' }));

    expect(onReview).toHaveBeenCalledExactlyOnceWith({
      comment: 'This is a test comment',
      state: ChangeRequestReviewType.COMMENT,
    });

    await waitFor(() => {
      expect(getByText('Your review was successfully submitted')).toBeInTheDocument();
    });
  });

  test('should fail to submit change request review', async () => {
    const onReview = vi.fn();
    const { getByRole, getByText } = renderWithMessageProvider(
      <>
        <Toaster />
        <ChangeRequestSubmitReview onReview={onReview} onDiscard={vi.fn()}/>
      </>,
    );

    onReview.mockThrowOnce(new Error('Test error'));

    await userEvents.click(getByRole('radio', { name: 'Request changes' }));
    await userEvents.click(getByRole('button', { name: 'Submit review' }));

    expect(onReview).toHaveBeenCalledExactlyOnceWith({
      comment: '',
      state: ChangeRequestReviewType.REQUEST_CHANGES,
    });

    await waitFor(() => {
      expect(getByText('Unexpected server error occurred')).toBeInTheDocument();
    });
  });
});
