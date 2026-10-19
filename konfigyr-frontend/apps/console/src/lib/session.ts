import { sealData, unsealData } from 'iron-session';
import { deleteCookie, getCookie, setCookie } from '@tanstack/react-start/server';
import { createLogger } from '@konfigyr/logger';

import type { UnsealErrorReason } from 'iron-session';

const logger = createLogger('services/session');

export type SessionCookieOptions = NonNullable<Parameters<typeof setCookie>[2]>;

export interface SessionOptions<T extends object> {
  /**
   * Name of the cookie that stores the sealed session data.
   */
  name: string;
  /**
   * Secret used to seal and unseal the session data. Must be at least 32 characters long.
   */
  password: string;
  /**
   * Session data fields that are read from the cookie. Other fields are ignored.
   */
  fields: Array<keyof T>;
  /**
   * Options used when the session cookie is written or deleted.
   */
  cookie: SessionCookieOptions;
}

export interface Session<T extends object> {
  readonly data: Partial<T>;
  update: (update: Partial<T>) => Promise<Session<T>>;
  clear: () => Promise<Session<T>>;
}

const onUnsealError = (reason: UnsealErrorReason, error: unknown) => {
  if (reason === 'expired') {
    logger.debug('Session cookie has expired, starting a new session');
  } else {
    logger.warn(error, `Session cookie could not be read (${reason}), starting a new session`);
  }
};

const pick = <T extends object>(data: Partial<T>, fields: Array<keyof T>): Partial<T> => {
  return fields.reduce((result, field) => {
    if (data[field] !== undefined) {
      result[field] = data[field];
    }
    return result;
  }, {} as Partial<T>);
};

/**
 * Creates the session for the current request from the sealed session cookie.
 *
 * Only the configured fields are read from the cookie. A missing, expired or unreadable
 * cookie results in an empty session.
 *
 * Updating the session seals the data and writes it to the session cookie.
 * Clearing the session deletes the cookie.
 *
 * @param options session cookie and sealing options
 * @returns the session for the current request
 */
export const createSession = async <T extends object>(options: SessionOptions<T>): Promise<Session<T>> => {
  const { name, password, fields, cookie } = options;

  const seal = getCookie(name);
  let data = pick(seal ? await unsealData<Partial<T>>(seal, { password, onUnsealError }) : {}, fields);

  const session: Session<T> = {
    get data() {
      return data;
    },
    update: async (update: Partial<T>) => {
      data = { ...data, ...update };
      setCookie(name, await sealData(data, { password }), cookie);
      return session;
    },
    clear: () => {
      data = {};
      deleteCookie(name, cookie);
      return Promise.resolve(session);
    },
  };

  return session;
};
