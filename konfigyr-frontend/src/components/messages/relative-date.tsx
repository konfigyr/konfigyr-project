import { useMemo } from 'react';
import { useIntl } from 'react-intl';

import type { ComponentProps } from 'react';

type UseTimeRange = (time: string | number | Date | undefined | null) =>
  { date: Date, value: number, unit: Intl.RelativeTimeFormatUnit } | null;

const useTimeRange: UseTimeRange = (value) => useMemo(() => {
  if (!value) {
    return null;
  }

  const now = Date.now();
  const date = new Date(value);
  const time = date.getTime();
  let diff = Math.abs(Math.ceil((time - now) / 1000));

  // anything less than 1 second is "just now"
  if (Math.abs(diff) <= 60) {
    return { date, value: 0, unit: 'second' };
  }

  const mark = (time > now) ? 1 : -1;

  // check difference less than 60 seconds
  if (Math.abs(diff) <= 60) {
    return { date, value: mark * diff, unit: 'seconds' };
  }

  diff = Math.ceil(diff / 60);
  // check difference less than 60 minutes
  if (Math.abs(diff) <= 60) {
    return { date, value: mark * diff, unit: 'minutes' };
  }

  diff = Math.ceil(diff / 60);
  // check difference less than 24 hours
  if (Math.abs(diff) <= 24) {
    return { date, value: mark * diff, unit: 'hours' };
  }

  diff = Math.ceil(diff / 24);
  // check difference less than 30 days
  if (Math.abs(diff) <= 30) {
    return { date, value: mark * diff, unit: 'days' };
  }

  // check difference less than 60 days
  if (Math.abs(diff) <= 60) {
    return { date, value: mark * Math.ceil(diff / 7), unit: 'weeks' };
  }

  diff = Math.ceil(diff / 30);
  // check difference less than 12 months
  if (Math.abs(diff) <= 12) {
    return { date, value: mark * diff, unit: 'months' };
  }

  return { date, value: mark * Math.ceil(diff / 12), unit: 'years' };
}, [value]);

export function RelativeDate({ value, title, ...props }: { value: string | number | Date | undefined | null} & ComponentProps<'time'>) {
  const params = useTimeRange(value);
  const intl = useIntl();

  if (params === null) {
    return null;
  }

  const name = title || intl.formatDate(params.date, {
    year: 'numeric',
    month: 'long',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });

  const label = params.value === 0 ? intl.formatMessage({
    defaultMessage: 'Just now',
    description: 'Label for relative time when the time difference is less than a minute',
  }) : intl.formatRelativeTime(params.value, params.unit);

  return (
    <time {...props} title={name} dateTime={params.date.toISOString()}>
      {label}
    </time>
  );
}
