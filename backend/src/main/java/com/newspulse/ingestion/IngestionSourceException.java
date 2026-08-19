package com.newspulse.ingestion;

public class IngestionSourceException extends RuntimeException {

	public IngestionSourceException(String message) {
		super(message);
	}

	public IngestionSourceException(String message, Throwable cause) {
		super(message, cause);
	}
}
