package org.thevlad.pwc.crawler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.html.HtmlEncodingDetector;

public class HtmlEncodingDetectorFromHeader extends HtmlEncodingDetector {

    private static final Pattern charsetPattern = Pattern.compile("(?i)\\bcharset=\\s*\"?([^\\s;\"]*)");

	public HtmlEncodingDetectorFromHeader() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Charset detect(InputStream input, Metadata metadata) throws IOException {
		if (metadata != null) {
			String contentType = metadata.get(Metadata.CONTENT_TYPE);
			if (contentType != null) {
				Matcher m = charsetPattern.matcher(contentType);
				if (m.find()) {
					String charsetName = m.group(1).trim().toUpperCase();
					Charset c = Charset.forName(charsetName);
					if (c != null) {
						return c;
					}
				}
			}
		}
		return super.detect(input, metadata);
	}

}
