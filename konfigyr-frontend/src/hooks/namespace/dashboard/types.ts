export interface DashboardSummary {
  activeServices: number;
  members: {
    count: number;
    limit: number | null;
  };
  openChangeRequests: number;
  activeConfigurations: number;
  artifactsOwned: number;
}
