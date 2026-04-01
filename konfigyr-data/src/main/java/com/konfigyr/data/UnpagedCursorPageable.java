package com.konfigyr.data;

enum UnpagedCursorPageable implements CursorPageable {

	INSTANCE;


	@Override
	public int size() {
		throw new UnsupportedOperationException("Can not get size of unpaged cursor pageable");
	}

	@Override
	public String token() {
		throw new UnsupportedOperationException("Can not get token of unpaged cursor pageable");
	}

	@Override
	public boolean isPaged() {
		return false;
	}

	@Override
	public String toString() {
		return "CursorPageable(unpaged)";
	}
}
