import { FormattedMessage } from 'react-intl';
import { DataUnit } from '@konfigyr/hooks/transforms';

export function SelectDataSizeUnitLabel() {
  return (
    <FormattedMessage
      defaultMessage="Select data size unit"
      description="Label for the dropdown or select menu to select the data size unit for the duration. Usually used in the data size input field group."
    />
  );
}

export function DataSizeUnitLabel({ unit }: { unit: DataUnit }) {
  switch (unit) {
    case DataUnit.BYTES:
      return (
        <FormattedMessage
          defaultMessage="Bytes"
          description="Label for the byte data size unit in the data size input field."
        />
      );
    case DataUnit.KILOBYTES:
      return (
        <FormattedMessage
          defaultMessage="Kilobytes"
          description="Label for the kilobyte data size unit in the data size input field."
        />
      );
    case DataUnit.MEGABYTES:
      return (
        <FormattedMessage
          defaultMessage="Megabytes"
          description="Label for the megabyte data size unit in the data size input field."
        />
      );
    case DataUnit.GIGABYTES:
      return (
        <FormattedMessage
          defaultMessage="Gigabytes"
          description="Label for the gigabyte data size unit in the data size input field."
        />
      );
    case DataUnit.TERABYTES:
      return (
        <FormattedMessage
          defaultMessage="Terabytes"
          description="Label for the terabyte data size unit in the data size input field."
        />
      );
  }
}
