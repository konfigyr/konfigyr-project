export interface Account {
  id: string,
  email: string,
  firstName?: string,
  lastName?: string,
  fullName?: string,
  avatar?: string,
  memberships: Array<Membership>,
  deletable?: boolean,
  lastLoginAt?: string,
  createdAt?: string,
  updatedAt?: string,
}

export interface Membership {
  id: string;
  namespace: string;
  name: string;
  avatar?: string;
  role: 'ADMIN' | 'USER';
  since: string;
}
