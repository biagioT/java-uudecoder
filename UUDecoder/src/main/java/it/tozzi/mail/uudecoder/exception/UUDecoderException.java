package it.tozzi.mail.uudecoder.exception;

import lombok.Getter;

/**
 * 
 * @author biagio.tozzi
 *
 */
public class UUDecoderException extends Exception {

	private static final long serialVersionUID = -8770023882303941631L;

	@Getter
	private String error;
	
	public UUDecoderException(String error, Exception e) {
		super(e);
		this.error = error;
	}
	
}
