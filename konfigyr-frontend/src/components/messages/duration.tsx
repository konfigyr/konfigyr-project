import { FormattedMessage } from 'react-intl';
import { DurationUnit } from '@konfigyr/hooks/transforms';

export function SelectDurationUnitLabel() {
  return (
    <FormattedMessage
      defaultMessage="Select time unit"
      description="Label for the dropdown or select menu to select the time unit for the duration. Usually used in the duration input field group."
    />
  );
}

export function DurationUnitLabel({ unit }: { unit: DurationUnit }) {
  switch (unit) {
    case DurationUnit.MICROSECONDS:
      return (
        <FormattedMessage
          defaultMessage="Microseconds"
          description="Label for the microsecond time unit in the duration input field."
        />
      );
    case DurationUnit.NANOSECONDS:
      return (
        <FormattedMessage
          defaultMessage="Nanoseconds"
          description="Label for the nanosecond time unit in the duration input field."
        />
      );
    case DurationUnit.MILLISECONDS:
      return (
        <FormattedMessage
          defaultMessage="Milliseconds"
          description="Label for the millisecond time unit in the duration input field."
        />
      );
    case DurationUnit.SECONDS:
      return (
        <FormattedMessage
          defaultMessage="Seconds"
          description="Label for the second time unit in the duration input field."
        />
      );
    case DurationUnit.MINUTES:
      return (
        <FormattedMessage
          defaultMessage="Minutes"
          description="Label for the minute time unit in the duration input field."
        />
      );
    case DurationUnit.HOURS:
      return (
        <FormattedMessage
          defaultMessage="Hours"
          description="Label for the hour time unit in the duration input field."
        />
      );
    case DurationUnit.DAYS:
      return (
        <FormattedMessage
          defaultMessage="Days"
          description="Label for the day time unit in the duration input field."
        />
      );
    default:
      return null;
  }
}
