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
