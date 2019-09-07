package org.thevlad.pwc.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thevlad.pwc.crawler.CrawlException;


@RestController
@ControllerAdvice
@RequestMapping(value = "/errors", produces = "application/json")

public class ErrorController {

	static final Logger logger = LoggerFactory.getLogger(ErrorController.class);
	
	private RestResponseMap<Map<String, List<String>>> validationErrorsResponseMap = new RestResponseMap<Map<String, List<String>>>();
	private RestResponseMap<String> simpleResponseMap = new RestResponseMap<String>();

	@RequestMapping
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public ResponseEntity<?> handle(HttpServletRequest request, Throwable ex) {
		logger.error("Internal Server Error: ",ex);
		return new ResponseEntity<>(simpleResponseMap.mapError(ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);

	}

	@RequestMapping("/404")
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public ResponseEntity<?> handle404(HttpServletRequest request, HttpServletResponse response) {
		String requestUri = (String) request.getAttribute("javax.servlet.error.request_uri");
		if (requestUri == null) {
			requestUri = "Unknown";
		}
		logger.debug("404: ",requestUri);
		String errMsg = "Resource not found: " + requestUri;
		return new ResponseEntity<>(simpleResponseMap.mapError(errMsg), HttpStatus.INTERNAL_SERVER_ERROR);
		
	}
	
	@ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
	ResponseEntity<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
		logger.error("Validationr Error: ",ex);
		
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        List<ObjectError> globalErrors = ex.getBindingResult().getGlobalErrors();
        
        Map<String, List<String>> validationErrors = new HashMap<String, List<String>>();
        for (FieldError fieldError : fieldErrors) {
        	List<String> fieldErrorList = null;
            String field = fieldError.getField();
            String fieldErrorMsg = fieldError.getDefaultMessage();
            if (validationErrors.containsKey(field)) {
            	fieldErrorList = validationErrors.get(field);
            } else {
            	fieldErrorList = new ArrayList<String>();
            	validationErrors.put(field, fieldErrorList);
            }
            fieldErrorList.add(fieldErrorMsg);
        }
        for (ObjectError objectError : globalErrors) {
        	List<String> objErrorList = null;
            String objName = objectError.getObjectName();
            String objErrorMsg = objectError.getDefaultMessage();
            if (validationErrors.containsKey(objName)) {
            	objErrorList = validationErrors.get(objName);
            } else {
            	objErrorList = new ArrayList<String>();
            	validationErrors.put(objName, objErrorList);
            }
            objErrorList.add(objErrorMsg);
        }
		return new ResponseEntity<>(validationErrorsResponseMap.mapError(validationErrors), HttpStatus.BAD_REQUEST);

    }
	
	@ExceptionHandler(CrawlException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	ResponseEntity<?> crawlException(CrawlException ex) {
		logger.error("Crawl Error: ", ex);
		return new ResponseEntity<>(simpleResponseMap.mapError(ex.getMessage()), HttpStatus.BAD_REQUEST);
	}
}
