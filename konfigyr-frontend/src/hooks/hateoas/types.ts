export interface CollectionResponse<T> {
  data: Array<T>;
}

export interface PageResponse<T> extends CollectionResponse<T> {
  metadata: {
    size?: number | null;
    number?: number | null;
    total?: number | null;
    pages?: number | null;
  }
}

export interface CursorResponse<T> extends CollectionResponse<T> {
  metadata: {
    size?: number | null;
    next?: string | null;
    previous?: string | null;
  }
}
