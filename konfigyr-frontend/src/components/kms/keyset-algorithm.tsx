import { FormattedMessage } from 'react-intl';
import { Slot } from 'radix-ui';
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectTrigger,
  SelectValue,
} from '@konfigyr/components/ui/select';
import { cn } from '@konfigyr/components/utils';
import { SupportedAlgorithm } from './supported-algorithms';

import type { ComponentProps, ReactNode } from 'react';

function AlgorithmItem({ algorithm, description, asChild = false, className, ...props }: {
  algorithm?: string | SupportedAlgorithm | ReactNode,
  description?: string | ReactNode,
  asChild?: boolean,
} & ComponentProps<'div'>) {
  const Comp = asChild ? Slot.Root : 'div';

  return (
    <Comp {...props}>
      {algorithm && (
        <p className={cn('text-sm leading-snug', className)}>
          {algorithm}
        </p>
      )}

      {description && (
        <p className="text-muted-foreground line-clamp-2 text-xs leading-normal font-normal text-balance">
          {description}
        </p>
      )}
    </Comp>
  );
}

export function KeysetAlgorithmName({ algorithm }: { algorithm?: string | SupportedAlgorithm }) {
  switch (algorithm) {
    case SupportedAlgorithm.AES128_GCM:
      return 'AES128-GCM';
    case SupportedAlgorithm.AES256_GCM:
      return 'AES256-GCM';
    case SupportedAlgorithm.ECDSA_P256:
      return 'ECDSA P-256';
    case SupportedAlgorithm.ECDSA_P384:
      return 'ECDSA P-384';
    case SupportedAlgorithm.ECDSA_P521:
      return 'ECDSA P-521';
    default:
      return null;
  }
}

export function KeysetAlgorithmDescription({ algorithm }: { algorithm?: string | SupportedAlgorithm }) {
  switch (algorithm) {
    case SupportedAlgorithm.AES128_GCM:
      return (
        <FormattedMessage
          defaultMessage="Encryption algorithm using AES with a 128-bit key in Galois Counter Mode (GCM)."
          description="Description of the AES128_GCM algorithm"
        />
      );
    case SupportedAlgorithm.AES256_GCM:
      return (
        <FormattedMessage
          defaultMessage="Encryption algorithm using AES with a 256-bit key in Galois Counter Mode (GCM). Recommended for encryption operations."
          description="Description of the AES256_GCM algorithm"
        />
      );
    case SupportedAlgorithm.ECDSA_P256:
      return (
        <FormattedMessage
          defaultMessage="Signing algorithm using ECDSA on the P-256 Curve with an SHA-256 digest. Recommended algorithm for digital signatures."
          description="Description of the ECDSA_P256 algorithm"
        />
      );
    case SupportedAlgorithm.ECDSA_P384:
      return (
        <FormattedMessage
          defaultMessage="Signing algorithm using ECDSA on the P-384 Curve with an SHA-512 digest."
          description="Description of the ECDSA_P384 algorithm"
        />
      );
    case SupportedAlgorithm.ECDSA_P521:
      return (
        <FormattedMessage
          defaultMessage="Signing algorithm using ECDSA on the P-521 Curve with an SHA-512 digest."
          description="Description of the ECDSA_P521 algorithm"
        />
      );
    default:
      return null;
  }
}

export function KeysetAlgorithmSelect({ value, reset = false, placeholder, detailed, onReset, onChange, ...props }: {
  value?: string | SupportedAlgorithm,
  reset?: boolean,
  detailed?: boolean,
  placeholder?: string | ReactNode,
  onChange?: (value: string | SupportedAlgorithm) => void,
  onReset?: () => void,
} & Omit<ComponentProps<typeof SelectTrigger>, 'onChange'>) {
  return (
    <Select value={value} onValueChange={onChange}>
      <SelectTrigger {...props}>
        <SelectValue placeholder={placeholder}>
          <KeysetAlgorithmName algorithm={value} />
        </SelectValue>
      </SelectTrigger>
      <SelectContent>
        <SelectGroup>
          <SelectLabel reset={reset} onReset={onReset}>
            <FormattedMessage
              defaultMessage="Algorithm"
              description="Label for the KMS keyset algorithm select dropdown menu."
            />
          </SelectLabel>
          {Object.values(SupportedAlgorithm).map((algorithm) => (
            <SelectItem key={algorithm} value={algorithm}>
              <KeysetAlgorithm algorithm={algorithm} detailed={detailed} />
            </SelectItem>
          ))}
        </SelectGroup>
      </SelectContent>
    </Select>
  );
}

export function KeysetAlgorithm({ algorithm, detailed = false }: { algorithm?: string | SupportedAlgorithm, detailed?: boolean}) {
  return (
    <AlgorithmItem
      algorithm={<KeysetAlgorithmName algorithm={algorithm} />}
      description={detailed && (
        <KeysetAlgorithmDescription algorithm={algorithm} />
      )}
      className={cn(detailed && 'font-medium')}
    />
  );
}
