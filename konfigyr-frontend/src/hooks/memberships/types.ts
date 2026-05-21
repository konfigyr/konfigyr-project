export enum NamespaceRole {
  ADMIN = 'ADMIN',
  USER = 'USER',
}

export interface Member {
  id: string;
  role: NamespaceRole;
  email: string;
  fullName: string;
  avatar?: string;
  since?: string;
}

export interface Invitation {
  key: string,
  organization: {
    id: string,
    slug: string,
    name: string,
    description?: string,
  },
  sender: {
    id: string,
    email: string,
    name?: string,
  },
  recipient: {
    id?: string,
    email: string,
    name?: string,
  },
  role: NamespaceRole,
  expired: boolean,
  createdAt: string | Date,
  expiryDate: string | Date,
}
