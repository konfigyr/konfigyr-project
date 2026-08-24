/**
 * Identifies the namespace module the currently matched route belongs to.
 *
 * Resolved once by {@link ModuleProvider} from the active route matches and exposed via
 * {@link useModule}, so the sidebar and the module switcher share a single source of truth
 * instead of independently matching against `useMatches()`.
 */
export type ModuleType = 'overview' | 'services' | 'artifactory' | 'kms';
