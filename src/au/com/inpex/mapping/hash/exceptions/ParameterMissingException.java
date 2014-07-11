package au.com.inpex.mapping.hash.exceptions;

public class ParameterMissingException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	

	public ParameterMissingException() {
	}

	public ParameterMissingException(String message) {
		super(message);
	}

	public ParameterMissingException(Throwable cause) {
		super(cause);
	}

	public ParameterMissingException(String message, Throwable cause) {
		super(message, cause);
	}

}
