import { FormattedMessage } from 'react-intl';
import { CodeIcon, GlobeIcon } from 'lucide-react';
import { Label } from '@konfigyr/components/ui/label';
import { RadioGroup, RadioGroupItem } from '@konfigyr/components/ui/radio-group';
import { cn } from '@konfigyr/components/utils';

import type { ReactNode } from 'react';
import type { VerificationMethod } from '@konfigyr/hooks/types';

const VERIFICATION_METHODS: Array<VerificationMethod> = ['DNS', 'SOURCE_CODE'];

const VERIFICATION_METHOD_ICON: Record<VerificationMethod, typeof GlobeIcon> = {
  DNS: GlobeIcon,
  SOURCE_CODE: CodeIcon,
};

const VERIFICATION_METHOD_DESCRIPTION: Record<VerificationMethod, ReactNode> = {
  DNS: (
    <FormattedMessage
      defaultMessage="Add a TXT record to your domain's DNS configuration to prove ownership."
      description="Description of the DNS group verification method."
    />
  ),
  SOURCE_CODE: (
    <FormattedMessage
      defaultMessage="Create a temporary repository for your groupId to verify ownership."
      description="Description of the SOURCE_CODE group verification method."
    />
  ),
};

export function VerificationMethodName({ method }: { method?: string | VerificationMethod }) {
  switch (method) {
    case 'DNS':
      return <FormattedMessage
        defaultMessage="DNS"
        description="Name or label of the DNS group verification method."
      />;
    case 'SOURCE_CODE':
      return <FormattedMessage
        defaultMessage="Source code"
        description="Name or label of the SOURCE_CODE group verification method."
      />;
    default:
      return null;
  }
}

export function GroupVerificationMethodSelector({ value, onChange }: {
  value?: VerificationMethod;
  onChange: (method: VerificationMethod) => void;
}) {
  return (
    <RadioGroup value={value ?? ''} onValueChange={(v) => onChange(v as VerificationMethod)}>
      {VERIFICATION_METHODS.map((method) => {
        const selected = value === method;
        const Icon = VERIFICATION_METHOD_ICON[method];
        return (
          <Label
            key={method}
            className={cn(
              'cursor-pointer border gap-4 rounded-xl p-4 transition-colors',
              selected ? 'border-primary bg-primary/5 dark:bg-primary/10' : 'border-transparent hover:bg-muted/50',
            )}
          >
            <RadioGroupItem value={method} />
            <div className="flex items-start gap-3">
              <Icon className="size-5 mt-0.5 shrink-0 text-muted-foreground" aria-hidden="true" />
              <div className="grid gap-1">
                <span className="font-medium leading-none">
                  <VerificationMethodName method={method} />
                </span>
                <p className="text-sm text-muted-foreground font-normal">{VERIFICATION_METHOD_DESCRIPTION[method]}</p>
              </div>
            </div>
          </Label>
        );
      })}
    </RadioGroup>
  );
}
