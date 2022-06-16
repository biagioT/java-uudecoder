package app.tozzi.uudecoder.exception;

/**
 * 
 * @author biagio.tozzi
 *
 */
public class UUDecoderException extends Exception {

	private static final long serialVersionUID = -8770023882303941631L;
	
	public UUDecoderException(String error, Exception e) {
		super(error, e);
	}
	
}
