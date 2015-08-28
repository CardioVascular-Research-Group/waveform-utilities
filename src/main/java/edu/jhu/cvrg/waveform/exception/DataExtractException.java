package edu.jhu.cvrg.waveform.exception;


public class DataExtractException extends Exception {

	private static final long serialVersionUID = -1874260566891662284L;
	
	private Level level = Level.ERROR;

	public DataExtractException() {
		// TODO Auto-generated constructor stub
	}

	public DataExtractException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}
	
	public DataExtractException(String message, Level level) {
		super(message);
		this.level = level;
	}

	public DataExtractException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	public DataExtractException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public DataExtractException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super();
		// TODO Auto-generated constructor stub
	}

	public Level getLevel() {
		return level;
	}

	
	public enum Level{
		ERROR,
		INFO;
	}
}

