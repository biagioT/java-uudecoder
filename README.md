
# JAVA UUDecoder

Utility for decoding uuencoded contents.
Extract text and attachments from an uuencoded mail.

**Usage**
```
<dependency>
	<groupId>app.tozzi</groupId>
	<artifactId>uudecoder</artifactId>
	<version>3.0</version>
</dependency>
```

*Simple example of use:*

    String uuencodedContent = ...;
    UUDecodedBean decodedBean = UUDecodedBean decode(content); 		 
    String content = decodedBean.getContent(); 
    List<UUDecodedAttachment> attachments = decodedBean.getAttachments();

*Advanced example*:

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


**Requirements**

 - [ ] >= Java 8

**License**
 - [ ] The project license file is available [here](https://github.com/biagioT/java-uudecoder/blob/master/LICENSE).