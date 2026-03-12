export interface Artifact {
  id: string;
  groupId: string;
  artifactId: string;
  version: string;
  name?: string;
  description?: string;
  website?: string;
  repository?: string;
}
