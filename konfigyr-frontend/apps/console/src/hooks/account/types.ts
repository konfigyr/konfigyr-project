export interface Account {
  id: string,
  email: string,
  firstName?: string,
  lastName?: string,
  fullName?: string,
  avatar?: string,
  deletable?: boolean,
  lastLoginAt?: string,
  createdAt?: string,
  updatedAt?: string,
}
