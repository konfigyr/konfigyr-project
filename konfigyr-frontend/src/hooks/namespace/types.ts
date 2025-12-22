export interface Namespace {
  id: string;
  name: string;
  slug: string;
  description?: string;
  avatar?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateNamespace {
  name?: string;
  slug?: string;
  description?: string;
}

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
  createdAt: string,
  expiryDate: string,
}
