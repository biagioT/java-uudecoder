package app.tozzi.uudecoder.model;

import java.util.ArrayList;
import java.util.List;

import app.tozzi.uudecoder.UUDecoder.UUDecodedAttachment;
import lombok.Data;

/**
 * 
 * @author biagio.tozzi
 *
 */
@Data
public class UUDecodedBean {
	
	private String content;
	private List<UUDecodedAttachment> attachments = new ArrayList<>();
	
}
