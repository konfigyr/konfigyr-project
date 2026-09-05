import type { DashboardSummary } from '@konfigyr/hooks/types';

export const konfigyrSummary: DashboardSummary = {
  activeServices: 4,
  members: {
    count: 12,
    limit: 25,
  },
  openChangeRequests: 3,
  activeConfigurations: 128,
  artifactsOwned: 7,
};

export const johnDoeSummary: DashboardSummary = {
  activeServices: 9,
  members: {
    count: 5,
    limit: null,
  },
  openChangeRequests: 2,
  activeConfigurations: 41,
  artifactsOwned: 6,
};
