import { FormattedMessage } from 'react-intl';
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
import type { KeysetPurpose } from '@konfigyr/hooks/kms/types';

function AlgorithmItem({ algorithm, description, className, ...props }: {
  algorithm?: ReactNode,
  description?: ReactNode,
} & ComponentProps<'div'>) {
  return (
    <div {...props}>
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
    </div>
  );
}

export function KeysetAlgorithmName({ algorithm }: { algorithm?: string | SupportedAlgorithm | null }) {
  return SupportedAlgorithm.valueOf(algorithm)?.label;
}

export function KeysetAlgorithmDescription({ algorithm }: { algorithm?: string | SupportedAlgorithm | null }) {
  switch (SupportedAlgorithm.valueOf(algorithm)) {
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

export function KeysetAlgorithmSelect({
  value,
  reset = false,
  placeholder,
  detailed,
  purpose,
  onReset,
  onChange,
  ...props
}: {
  value?: string,
  reset?: boolean,
  detailed?: boolean,
  placeholder?: string | ReactNode,
  purpose?: KeysetPurpose,
  onChange?: (value: string | null) => void,
  onReset?: () => void,
} & Omit<ComponentProps<typeof SelectTrigger>, 'onChange'>) {
  const algorithm = SupportedAlgorithm.valueOf(value);

  return (
    <Select value={algorithm?.name} onValueChange={onChange}>
      <SelectTrigger {...props}>
        <SelectValue>
          {algorithm ? algorithm.label : placeholder}
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
          {SupportedAlgorithm.values(purpose).map((it) => (
            <SelectItem key={it.name} value={it.name}>
              <KeysetAlgorithm algorithm={it} detailed={detailed} />
            </SelectItem>
          ))}
        </SelectGroup>
      </SelectContent>
    </Select>
  );
}

export function KeysetAlgorithm({ algorithm, detailed = false }: { algorithm?: string | SupportedAlgorithm, detailed?: boolean }) {
  const value = SupportedAlgorithm.valueOf(algorithm);

  return (
    <AlgorithmItem
      algorithm={value?.label}
      description={detailed && (
        <KeysetAlgorithmDescription algorithm={value} />
      )}
      className={cn(detailed && 'font-medium')}
    />
  );
}
