# java-uudecoder
Utility for decoding uuencoded (mail) attachments. Example of use:

    //  MimePart part = ...
    if (part.isMimeType("text/plain")) {
		
       Object partContent = part.getContent();
       String txtContent = partContent.toString();

       if (UUDecoder.containsUUEncodedAttachments(txtContent)) {
	        txtContent = txtContent.substring(0, UUDecoder.getNextBeginIndex(txtContent));

	        for (UUDecodedAttachment uda : UUDecoder.getUUDecodedAttachments(partContent.toString())) {
		         String fileName = uda.getFileName();
		         DataSource dataSource = uda.getDataSource();
		         // ...
	        }
       }
		
     // ...
     String txtBody = MimeUtility.decodeText(txtContent);
    }
    // ...
