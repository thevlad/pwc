package org.thevlad.pwc.web;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

	private String errorMessage;
	private ErrorCode errorCode;
	
	public static enum ErrorCode {
		CRAWL_FATAL_ERROR, DOMAIN_ALREADY_CRAWLED
	}
}
