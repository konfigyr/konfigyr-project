import { z } from 'zod';
import { toast } from 'sonner';
import { useCallback, useId } from 'react';
import { FormattedMessage } from 'react-intl';
import {
  GitPullRequestClosedIcon,
} from 'lucide-react';
import { Editor } from '@konfigyr/components/editor';
import { useErrorNotification } from '@konfigyr/components/error';
import { ChangeRequestReviewType } from '@konfigyr/hooks/vault/types';
import {
  Card,
  CardContent,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@konfigyr/components/ui/card';
import { RadioGroup, RadioGroupItem } from '@konfigyr/components/ui/radio-group';
import {
  Field,
  FieldContent,
  FieldDescription,
  FieldLabel,
} from '@konfigyr/components/ui/field';
import {
  useForm,
  useFormSubmit,
} from '@konfigyr/components/ui/form';
import { Button } from '@konfigyr/components/ui/button';
import {
  ChangeRequestReviewTypeDescription,
  ChangeRequestReviewTypeLabel,
  DiscardChangeRequestReviewLabel,
  SubmitChangeRequestReviewLabel,
} from './messages';

import type { SubmitChangeRequestReview } from '@konfigyr/hooks/vault/types';

const reviewSchema = z.object({
  comment: z.string(),
  type: z.enum([
    ChangeRequestReviewType.APPROVE,
    ChangeRequestReviewType.COMMENT,
    ChangeRequestReviewType.REQUEST_CHANGES,
  ]),
});

function ChangeRequestReviewTypeRadioGroup({ value, onChange }: { value: string, onChange: (value: string) => void }) {
  const id = useId();

  return (
    <RadioGroup value={value} onValueChange={onChange}>
      {Object.values(ChangeRequestReviewType).map(type => (
        <Field key={type} orientation="horizontal">
          <RadioGroupItem id={id + type} value={type} />
          <FieldContent>
            <FieldLabel htmlFor={id + type}>
              <ChangeRequestReviewTypeLabel type={type} />
            </FieldLabel>
            <FieldDescription>
              <ChangeRequestReviewTypeDescription type={type} />
            </FieldDescription>
          </FieldContent>
        </Field>
      ))}
    </RadioGroup>
  );
}

export function ChangeRequestSubmitReview({ onDiscard, onReview }: {
  onDiscard: () => void | Promise<any>;
  onReview: (review: SubmitChangeRequestReview) => void | Promise<any>;
}) {
  const errorNotification = useErrorNotification();
  const form = useForm({
    defaultValues: {
      type: '',
      comment: '',
    },
    validators: {
      onChange: reviewSchema,
      onMount: reviewSchema,
    },
    onSubmit: async ({ value }) => {
      try {
        await onReview({
          state: value.type as ChangeRequestReviewType,
          comment: value.comment,
        });
      } catch (error) {
        return errorNotification(error);
      }

      toast.success((
        <FormattedMessage
          defaultMessage="Your review was successfully submitted"
          description="Notification message that is shown when user successfully submits a change request review."
        />
      ));

      form.reset();
    },
  });

  const onSubmit = useFormSubmit(form);
  const onDiscardChangeRequest = useCallback(async () => {
    try {
      await onDiscard();
    } catch (error) {
      return errorNotification(error);
    }

    toast.success((
      <FormattedMessage
        defaultMessage="Change request was discarded"
        description="Notification message shown when change request was discarded."
      />
    ));
  }, [onDiscard]);

  return (
    <form.AppForm>
      <form name="change-request-review-form" onSubmit={onSubmit}>
        <Card className="border">
          <CardHeader>
            <CardTitle>
              <FormattedMessage
                defaultMessage="Review change request"
                description="Title of the change request review form. Should instruct users to submit a review for the change request."
              />
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <form.AppField
              name="comment"
              children={(field) => (
                <field.Control
                  render={<Editor
                    name="change-request-review-comment"
                    value={field.state.value}
                    placeholder={
                      <FormattedMessage
                        defaultMessage="Leave a comment (optional)"
                        description="Placeholder for the change request review comment field"
                      />
                    }
                    onValueChange={field.handleChange}
                    onBlur={field.handleBlur}
                  />}
                />
              )}
            />

            <form.AppField
              name="type"
              children={(field) => (
                <ChangeRequestReviewTypeRadioGroup
                  value={field.state.value}
                  onChange={field.handleChange}
                />
              )}
            />
          </CardContent>
          <CardFooter className="justify-end gap-2">
            <Button variant="outline" onClick={onDiscardChangeRequest}>
              <GitPullRequestClosedIcon className="text-destructive" />
              <DiscardChangeRequestReviewLabel />
            </Button>

            <form.Submit>
              <SubmitChangeRequestReviewLabel />
            </form.Submit>
          </CardFooter>
        </Card>
      </form>
    </form.AppForm>
  );
}
