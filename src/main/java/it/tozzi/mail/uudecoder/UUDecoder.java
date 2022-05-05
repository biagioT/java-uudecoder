package it.tozzi.mail.uudecoder;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.activation.DataSource;
import javax.activation.FileTypeMap;
import javax.mail.internet.MimeUtility;
import javax.mail.util.ByteArrayDataSource;

import it.tozzi.mail.uudecoder.exception.UUDecoderException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author biagio.tozzi
 *
 */
@Slf4j
public class UUDecoder {

	private static final String CONTENT_TYPE_OCETSTREAM = "application/octet-stream";

	/**
	 * Extracts uudecoded attachments from EML text content
	 * 
	 * @param content EML text content (text/plain)
	 * @return List of {@link UUDecodedAttachment}
	 * @throws UUDecoderException 
	 */
	public static List<UUDecodedAttachment> getUUDecodedAttachments(String content) throws UUDecoderException {
		List<UUDecodedAttachment> res = new ArrayList<>();
		getUUDecodedAttachments(content, res);
		return res;
	}

	/**
	 * Extracts next uuencoded string from EML text content
	 * 
	 * @param content EML text content (text/plain)
	 * @return next uuencoded string
	 * @throws UUDecoderException 
	 */
	public static String getNextUUEncodedString(String content) throws UUDecoderException {

		if (content == null)
			return null;
		
		int beginIndex = content.indexOf("begin ");
		int endIndex = content.indexOf("end");

		if (beginIndex == -1 || endIndex == -1)
			return null;

		if (beginIndex > endIndex) {
			return getNextUUEncodedString((content.substring(beginIndex)));
		}

		String subString = content.substring(beginIndex, endIndex + 3);

		if (internalContainsUUencodedAttachments(subString)) {
			return subString;
		}

		return getNextUUEncodedString(content.substring(beginIndex + 6));
	}

	/**
	 * Finds the first index containing the identifier <i>begin</i>
	 * 
	 * @param content EML text content (text/plain)
	 * @return first index containing the identifier <i>begin</i>
	 * @throws UUDecoderException 
	 */
	public static int getNextBeginIndex(String content) throws UUDecoderException {

		if (content == null)
			return -1;
		
		int beginIndex = content.indexOf("begin ");
		int endIndex = content.indexOf("end");

		if (beginIndex == -1 || endIndex == -1)
			return -1;

		if (beginIndex > endIndex) {
			return getNextBeginIndex(content.substring(endIndex + 3));
		}

		String subString = content.substring(beginIndex, endIndex + 3);

		if (internalContainsUUencodedAttachments(subString)) {
			return beginIndex;
		}

		return getNextBeginIndex(content.substring(beginIndex + 6));
	}

	/**
	 * Check if the content has uuencoded attachments
	 * 
	 * @param content EML text content (text/plain)
	 * @return true if the content has uuencoded attachments, false otherwise
	 * @throws UUDecoderException 
	 */
	public static boolean containsUUEncodedAttachments(String content) throws UUDecoderException {

		if (content == null)
			return false;
		
		int beginIndex = content.indexOf("begin ");
		int endIndex = content.indexOf("end");

		if (beginIndex == -1 || endIndex == -1)
			return false;

		if (beginIndex > endIndex) {
			return containsUUEncodedAttachments(content.substring(beginIndex));
		}

		String subString = content.substring(beginIndex, endIndex + 3);

		if (!internalContainsUUencodedAttachments(subString)) {
			return containsUUEncodedAttachments(content.substring(beginIndex + 6));
		}

		return true;

	}

	private static void getUUDecodedAttachments(String content, List<UUDecodedAttachment> res) throws UUDecoderException {

		if (content == null)
			return;
		
		int beginIndex = content.indexOf("begin ");
		int endIndex = content.indexOf("end");

		if (beginIndex == -1 || endIndex == -1)
			return;

		if (beginIndex > endIndex) {
			getUUDecodedAttachments(((content.substring(beginIndex))), res);

		} else {
			String subString = content.substring(beginIndex, endIndex + 3);

			if (internalContainsUUencodedAttachments(subString)) {
				InputStream is = null;
				InputStream isDecoded = null;
				String fileName = null;

				try {
					fileName = getUUDecodedFileName(subString, res);
					is = new ByteArrayInputStream(content.getBytes());
					isDecoded = MimeUtility.decode(is, "uuencode");
					res.add(new UUDecodedAttachment(fileName, createDataSource(fileName, isDecoded)));

				} catch (Exception e) {
					log.error("Error during decoding of attachment {}", fileName, e);
					throw new UUDecoderException("Error during decoding of attachment " + fileName, e);

				} finally {
					closeStreams(is);
				}

			}

			getUUDecodedAttachments(((content.substring(beginIndex + 6))), res);
		}
	}

	private static boolean internalContainsUUencodedAttachments(String content) throws UUDecoderException {
		BufferedReader reader = null;

		try {
			reader = new BufferedReader(new StringReader(content));

			if (!reader.ready())
				throw new UUDecoderException("Error during uuencoded content check: reader not ready", null);

			String firstLine = reader.readLine();
			if (firstLine == null || !firstLine.regionMatches(false, 0, "begin ", 0, 6))
				return false;

			String lastLine = reader.lines().reduce((first, second) -> second).orElse(null);
			if (lastLine == null || !lastLine.regionMatches(false, 0, "end", 0, 3))
				return false;

			Integer mode = null;

			try {
				mode = Integer.parseInt(firstLine.substring(6, 9));

			} catch (NumberFormatException e) {
				log.warn("Permissions mode not valid", e);
				return false;
			}

			if (!isOctal(mode)) {
				log.warn("Permissions mode not in octal format");
				return false;
			}

			String fileName = firstLine.substring(9);
			if (fileName == null || fileName.trim().isEmpty()) {
				log.warn("File name not present");
				return false;
			}

			return true;

		} catch (Exception e) {
			log.error("Error during uuencoded content check", e);
			throw new UUDecoderException("Error during uuencoded content check", e);

		} finally {
			closeReaders(reader);
		}
	}

	private static String getUUDecodedFileName(String content, List<UUDecodedAttachment> res) {

		BufferedReader reader = null;

		try {
			reader = new BufferedReader(new StringReader(content));
			return reader.readLine().substring(9).trim();

		} catch (Exception e) {
			log.error("Error during reading file name", e);
			return getVersionedFileName("unnamed",
					res.stream().map(uda -> uda.getFileName()).collect(Collectors.toList()), 1);

		} finally {
			closeReaders(reader);
		}
	}

	private static String getVersionedFileName(String fileName, List<String> files, int i) {

		if (files.contains(fileName)) {
			fileName = fileName + "(" + i + ")";
			return getVersionedFileName(fileName, files, i++);
		}

		return fileName;
	}

	private static boolean isOctal(Integer number) {
		boolean isOctal = false;

		while (number > 0) {
			if (number % 10 <= 7) {
				isOctal = true;
			} else {
				isOctal = false;
				break;
			}
			number /= 10;

		}

		return isOctal;
	}

	private static DataSource createDataSource(String fileName, InputStream inputStream) throws IOException {
		FileTypeMap map = FileTypeMap.getDefaultFileTypeMap();
		String type = map.getContentType(fileName);
		byte[] content = getContent(inputStream);
		ByteArrayDataSource result = new ByteArrayDataSource(content, type != null ? type : CONTENT_TYPE_OCETSTREAM);
		result.setName(fileName);
		return result;
	}

	private static byte[] getContent(final InputStream is) throws IOException {
		byte[] result;
		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		fastCopy(is, os);
		result = os.toByteArray();
		return result;
	}

	private static void fastCopy(final InputStream src, final OutputStream dest) throws IOException {
		final ReadableByteChannel inputChannel = Channels.newChannel(src);
		final WritableByteChannel outputChannel = Channels.newChannel(dest);
		fastCopy(inputChannel, outputChannel);
		inputChannel.close();
		outputChannel.close();
	}

	private static void fastCopy(final ReadableByteChannel src, final WritableByteChannel dest) throws IOException {
		final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);

		while (src.read(buffer) != -1) {
			buffer.flip();
			dest.write(buffer);
			buffer.compact();
		}

		buffer.flip();

		while (buffer.hasRemaining()) {
			dest.write(buffer);
		}
	}

	private static void closeStreams(InputStream... streams) {
		for (InputStream stream : streams) {
			if (stream != null) {
				try {
					stream.close();

				} catch (IOException e) {
				}
			}
		}
	}

	private static void closeReaders(Reader... readers) {
		for (Reader reader : readers) {
			if (reader != null) {
				try {
					reader.close();

				} catch (IOException e) {
				}
			}
		}
	}

	@Data
	@AllArgsConstructor
	public static class UUDecodedAttachment {
		private String fileName;
		private DataSource dataSource;
	}
}
