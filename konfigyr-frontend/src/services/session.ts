import { getIronSession, IronSession, SessionOptions } from 'iron-session';

const cookieOptions = {
  secure: process.env.NODE_ENV === 'production',
};

/**
 * The high-level type definition of the Cookie Store API.
 */
export interface CookieStore {
  get: (name: string) => { name: string; value: string; } | undefined;
  set: {
    (name: string, value: string): void;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (options: any): void;
  };
}

type Session = IronSession<Record<string, string>>;

/**
 * Session service that is used as a wrapper around the `iron-session` library. To create a new session
 * you need to provide the session name and a secret key to encrypt and decrypt the session data.
 */
export class SessionService {
  #options: SessionOptions;

  constructor(name: string, secret?: string) {
    this.#options = { cookieName: name, password: secret || crypto.randomUUID(), cookieOptions };
  }

  /**
   * Retrieves the session value from the HTTP cookie store by its name.
   *
   * @param {CookieStore} cookies HTTP cookies
   * @param {String} name name of the session value
   * @return {Promise} promise containing the session value or undefined if not found
   */
  async get<T>(cookies: CookieStore, name: string): Promise<T | undefined> {
    const session: Session = await getIronSession(cookies, this.#options);
    const value = session[name];
    return value && JSON.parse(value);
  }

  /**
   * Stores the session value in the HTTP cookie store by its name.
   *
   * @param {CookieStore} cookies HTTP cookies
   * @param {String} name name of the session value
   * @param {Object} value value to be stored in the session
   */
  async set<T>(cookies: CookieStore, name: string, value: T) {
    const session: Session = await getIronSession(cookies, this.#options);
    session[name] = JSON.stringify(value);
    await session.save();
  }

  /**
   * Removes the session value from the HTTP cookie store by its name.
   *
   * @param {CookieStore} cookies HTTP cookies
   * @param {String} name name of the session value
   */
  async remove(cookies: CookieStore, name: string) {
    const session: Session = await getIronSession(cookies, this.#options);
    delete session[name];
    await session.save();
  }

  /**
   * Clears all the session values from the HTTP cookie store.
   *
   * @param {CookieStore} cookies HTTP cookies
   */
  async clear(cookies: CookieStore) {
    const session: Session = await getIronSession(cookies, this.#options);
    session.destroy();
  }
}

const session = new SessionService('konfigyr.session', process.env.KONFIGYR_DEFAULT_SESSION_SECRET);

/**
 * Retrieves the session value by its name from the default `konfigyr.session` extracted from HTTP cookie store.
 *
 * @param {CookieStore} cookies HTTP cookies
 * @param {String} name name of the session value
 * @return {Promise} promise containing the session value or undefined if not found
 */
export async function get<T>(cookies: CookieStore, name: string): Promise<T | undefined> {
  return session.get(cookies, name);
}

/**
 * Stores the session value into the `konfigyr.session` extracted from the HTTP cookie store.
 *
 * @param {CookieStore} cookies HTTP cookies
 * @param {String} name name of the session value
 * @param {Object} value value to be stored in the session
 */
export async function set<T>(cookies: CookieStore, name: string, value: T) {
  return session.set(cookies, name, value);
}

/**
 * Removes the session value from the `konfigyr.session` extracted from the HTTP cookie store.
 *
 * @param {CookieStore} cookies HTTP cookies
 * @param {String} name name of the session value
 */
export async function remove(cookies: CookieStore, name: string) {
  return session.remove(cookies, name);
}
