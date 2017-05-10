package net.jotorren.microservices.tx;

public class CompositeTransactionException extends Exception {
	private static final long serialVersionUID = 3758500701912008969L;

	public CompositeTransactionException() {
	}

	public CompositeTransactionException(String message) {
		super(message);
	}

	public CompositeTransactionException(Throwable cause) {
		super(cause);
	}

	public CompositeTransactionException(String message, Throwable cause) {
		super(message, cause);
	}

	public CompositeTransactionException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
