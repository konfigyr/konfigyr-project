'use client';

import { z } from 'zod';
import { toast } from 'sonner';
import { Send } from 'lucide-react';
import { useTranslations } from 'next-intl';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  Button,
  Input,
  Textarea,
  Form,
  FormField,
  FormControl,
  useForm,
} from 'konfigyr/components/ui';

export interface ContactInformation {
  name?: string;
  email: string;
  subject?: string;
  message: string;
}

export type ContactInformationAction = (data: ContactInformation) => Promise<{ error: boolean }>;

const contactFormSchema = z.object({
  name: z.string().optional(),
  email: z.string().email(),
  subject: z.string().optional(),
  message: z.string().nonempty(),
});

export default function ContactForm({ action }: { action: ContactInformationAction }) {
  const t = useTranslations('contact.form');
  const form = useForm<z.infer<typeof contactFormSchema>>({
    defaultValues: { name: '', email: '', subject: '', message: '' },
    resolver: zodResolver(contactFormSchema),
  });

  const handler = async (data: ContactInformation) => {
    const { error = false } = await action(data);

    if (error) {
      toast.error(t('error.title'), { description: t('error.detail') });
    } else {
      toast.success(t('success.title'), { description: t('success.detail') });
    }
  };

  return (
    <Form {...form}>
      <form name="contact-form" className="grid gap-6" onSubmit={form.handleSubmit(handler)}>
        <div className="grid grid-cols-2 gap-4">
          <FormControl
            name="name"
            render={({field}) => (
              <FormField label={t('name.label')}>
                <Input placeholder={t('name.placeholder')} {...field}/>
              </FormField>
            )}
          />

          <FormControl
            name="email"
            render={({field}) => (
              <FormField label={t('email.label')}>
                <Input type="email" placeholder={t('email.placeholder')} {...field}/>
              </FormField>
            )}
          />
        </div>
        <div className="space-y-2">
          <FormControl
            name="subject"
            render={({field}) => (
              <FormField label={t('subject.label')}>
                <Input placeholder={t('subject.placeholder')} {...field}/>
              </FormField>
            )}
          />
        </div>
        <div className="space-y-2">
          <FormControl
            name="message"
            render={({field}) => (
              <FormField label={t('message.label')}>
                <Textarea rows={6} placeholder={t('message.placeholder')} {...field}/>
              </FormField>
            )}
          />
        </div>
        <Button
          type="submit"
          className="w-full"
          disabled={form.formState.disabled || !form.formState.isValid}
          loading={form.formState.isSubmitting || form.formState.isValidating}
        >
          <Send className="ml-2 h-4 w-4"/> Send Message
        </Button>
      </form>
    </Form>
  );
}
