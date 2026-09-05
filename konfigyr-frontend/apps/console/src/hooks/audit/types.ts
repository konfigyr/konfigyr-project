export interface AuditActor {
  id: string
  type: string & ('USER_ACCOUNT' | 'OAUTH_CLIENT' | 'SYSTEM'),
  name: string;
}

export interface AuditRecord {
  id: string;
  entityType: string;
  entityId: string;
  eventType: string;
  actor: AuditActor;
  message: string;
  details?: Record<string, any>;
  createdAt: string;
}

export interface AuditRecordQuery {
  entityType?: string;
  entityId?: string;
  from?: string;
  to?: string;
  token?: string;
  size?: number;
}
