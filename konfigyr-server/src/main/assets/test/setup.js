import { vi } from 'vitest';
import createFetchMock from 'vitest-fetch-mock';
import { LiveRegionElement } from '@primer/live-region-element';
import '@testing-library/jest-dom/vitest';

// for some reason the live-region is not registered as a custom element
// automatically using the `@primer/live-region-element/define` script
if (!customElements.get('live-region')) {
    customElements.define('live-region', LiveRegionElement);
}

// mock the Fetch API for all tests via `fetchMock` global variable
createFetchMock(vi).enableMocks();
