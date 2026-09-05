import type {
  KeyType,
  KeysetAlgorithm,
  KeysetOperation,
  KeysetPurpose,
} from '@konfigyr/hooks/kms/types';

export class SupportedAlgorithm implements KeysetAlgorithm {
  static AES128_GCM = new SupportedAlgorithm('AES128_GCM', 'AES128-GCM', 'OCTET', 'ENCRYPTION', ['encrypt', 'decrypt']);
  static AES256_GCM = new SupportedAlgorithm('AES256_GCM', 'AES256-GCM', 'OCTET', 'ENCRYPTION', ['encrypt', 'decrypt']);
  static ECDSA_P256 = new SupportedAlgorithm('ECDSA_P256', 'ECDSA P-256', 'EC', 'SIGNING', ['sign', 'verify']);
  static ECDSA_P384 = new SupportedAlgorithm('ECDSA_P384', 'ECDSA P-384', 'EC', 'SIGNING', ['sign', 'verify']);
  static ECDSA_P521 = new SupportedAlgorithm('ECDSA_P521', 'ECDSA P-521', 'EC', 'SIGNING', ['sign', 'verify']);

  static values(purpose?: KeysetPurpose): Array<SupportedAlgorithm> {
    const algorithms = [
      SupportedAlgorithm.AES128_GCM,
      SupportedAlgorithm.AES256_GCM,
      SupportedAlgorithm.ECDSA_P256,
      SupportedAlgorithm.ECDSA_P384,
      SupportedAlgorithm.ECDSA_P521,
    ];

    if (purpose) {
      return algorithms.filter((it) => it.purpose === purpose);
    }

    return algorithms;
  }

  static valueOf(value?: string | SupportedAlgorithm | null) {
    if (value instanceof SupportedAlgorithm) {
      return value;
    }

    switch (value) {
      case SupportedAlgorithm.AES128_GCM.name:
        return SupportedAlgorithm.AES128_GCM;
      case SupportedAlgorithm.AES256_GCM.name:
        return SupportedAlgorithm.AES256_GCM;
      case SupportedAlgorithm.ECDSA_P256.name:
        return SupportedAlgorithm.ECDSA_P256;
      case SupportedAlgorithm.ECDSA_P384.name:
        return SupportedAlgorithm.ECDSA_P384;
      case SupportedAlgorithm.ECDSA_P521.name:
        return SupportedAlgorithm.ECDSA_P521;
      default:
        return null;
    }
  }

  constructor(
    public readonly name: string,
    public readonly label: string,
    public readonly type: KeyType,
    public readonly purpose: KeysetPurpose,
    public readonly operations: Array<KeysetOperation> = [],
  ) {}

  toString() {
    return this.name;
  }
}
