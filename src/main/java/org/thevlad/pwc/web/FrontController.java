package org.thevlad.pwc.web;

import java.io.IOException;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.thevlad.pwc.crawler.CrawlException;
import org.thevlad.pwc.crawler.CrawlService;
import org.thevlad.pwc.index.Indexer;
import org.thevlad.pwc.index.SearchRequest;
import org.thevlad.pwc.index.SearchResult;
import org.thevlad.pwc.services.CounterService;
import org.thevlad.pwc.services.CounterState;
import org.thevlad.pwc.web.ErrorResponse.ErrorCode;

import edu.uci.ics.crawler4j.url.WebURL;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@RestController
@CrossOrigin
@Slf4j
public class FrontController {

	public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.106 Safari/537.36";

	private static final String SESSION_COOKIE_NAME = "JSESSIONID";
	private static final String SESSION_DATA_KEY = "_CuStOm_Session_Data";

	private RestResponseMap<Map<String, String>> simpleMapResponseMap = new RestResponseMap<Map<String, String>>();
	private RestResponseMap<String> simpleResponseMap = new RestResponseMap<String>();
	private RestResponseMap<SearchResponse> searchResponseMap = new RestResponseMap<SearchResponse>();
	private RestResponseMap<SearchResult> searchResultResponseMap = new RestResponseMap<SearchResult>();
	private RestResponseMap<ErrorResponse> errorResponseMap = new RestResponseMap<ErrorResponse>();

	@Autowired
	CounterService counter;
	@Autowired
	private CrawlService crawlService;
	@Autowired
	private Indexer indexer;

	@RequestMapping(value = "/crawl", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> crawl(@RequestBody @Valid CrawlRequest crawlRequest, HttpServletRequest request)
			throws CrawlException {
		HttpSession session = request.getSession();
		if (!crawlRequest.getAddress().startsWith("http")) {
			String addr = crawlRequest.getAddress();
			addr = "http://" + addr;
			crawlRequest.setAddress(addr);
		}
		isValidURL(crawlRequest.getAddress());
		CustomSessionData sessionData = (CustomSessionData) session.getAttribute(SESSION_DATA_KEY);
		String domain = extractDomain(crawlRequest.getAddress());
		Map<String, String> domainMap = new HashMap<String, String>();
		domainMap.put("domain", domain);
		if (sessionData != null) {
			if (sessionData.isDomainWasSubmitted(domain)) {
//				String errMsg = "Site " + crawlRequest.getAddress() + " already has been submitted in the current session!";
				return new ResponseEntity<>(simpleMapResponseMap.mapOK(domainMap), HttpStatus.OK);
//				return new ResponseEntity<>(simpleResponseMap.mapError(errMsg), HttpStatus.BAD_REQUEST);				
			}
		} else {
			sessionData = new CustomSessionData();
			session.setAttribute(SESSION_DATA_KEY, sessionData);
		}
		try {
			if (needCrawl(domain)) {
				log.debug("URL not in index {}", crawlRequest.getAddress());
				crawlService.crawl(crawlRequest.getAddress());
				sessionData.addCrawlRequst(extractDomain(crawlRequest.getAddress()));
				sessionData.setCurrentDomain(domain);
			} else {
				ErrorResponse errorResponse = new ErrorResponse("Domain " + domain + " already crawled", ErrorCode.DOMAIN_ALREADY_CRAWLED);
				return new ResponseEntity<>(errorResponseMap.mapError(errorResponse), HttpStatus.BAD_REQUEST);
			}
			return new ResponseEntity<>(simpleMapResponseMap.mapOK(domainMap), HttpStatus.OK);
		} catch (CrawlException e) {
			log.error("Error to crawl" + crawlRequest.getAddress(), e);
			ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), ErrorCode.CRAWL_FATAL_ERROR);
			return new ResponseEntity<>(errorResponseMap.mapError(errorResponse), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping(path = { "/start" }, produces = { MediaType.APPLICATION_JSON_UTF8_VALUE })
	public ResponseEntity<String> initSession(HttpServletRequest request, HttpServletResponse response) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		} else {
			session = request.getSession(true);
		}
		String sessionId = session.getId();
//		handleSessionCookie(request, response);
		return ResponseEntity.of(Optional.of(String.format("{ \"sessionId\": \"%s\" }", sessionId)));
	}

	private void handleSessionCookie(HttpServletRequest request, HttpServletResponse response) {
		HttpSession oldSession = request.getSession(false);
		if (oldSession != null) {
			System.out.println("Detect old session: " + oldSession.getId());
//			oldSession.invalidate();
		} else {
			System.out.println("No old session was detected");
		}

		HttpSession session = request.getSession();
		String sessionId = session.getId();
		System.out.println("New session ID: " + sessionId);
		/*
		 * Cookie[] cookies = request.getCookies(); if (cookies != null) { boolean found
		 * = false; for (int i = 0; i < cookies.length; i++) { Cookie cookie =
		 * cookies[i]; if (cookie.getName().equals(SESSION_COOKIE_NAME) &&
		 * sessionId.equals(cookie.getValue())) { found = true; break; } } if (!found) {
		 * Cookie cookie = new Cookie(SESSION_COOKIE_NAME, sessionId); String domain =
		 * request.getServerName().replaceAll(".*\\.(?=.*\\.)", "");
		 * cookie.setDomain(domain); response.addCookie(cookie); System.out.
		 * println("handleSessionCookie: cookie not found - new created with session ID = "
		 * + sessionId); } else { System.out.
		 * println("handleSessionCookie: session cookie found with session ID = " +
		 * sessionId); } } else { Cookie cookie = new Cookie(SESSION_COOKIE_NAME,
		 * sessionId); String domain =
		 * request.getServerName().replaceAll(".*\\.(?=.*\\.)", "");
		 * cookie.setDomain(domain); response.addCookie(cookie); System.out.
		 * println("handleSessionCookie: cookie not found - new created with session ID = "
		 * + sessionId); }
		 */
//		response.addCookie(cookie);
	}

	/*
	 * 
	 * 
	 * @GetMapping("/stream-sse") public Flux<ServerSentEvent<String>>
	 * streamEvents(HttpServletRequest request) {
	 * 
	 * HttpSession session = request.getSession(); String sessionId =
	 * session.getId(); System.out.printf("Session ID: %s\n", sessionId);
	 * System.out.printf("Request thread ID: %d\n", Thread.currentThread().getId());
	 * if (counter.getCounter(sessionId) == -1) { counter.addCounter(sessionId); }
	 * System.out.printf("New counter value is: %d\n", getCurrentCount(sessionId));
	 * 
	 * return Flux .interval(Duration.ofSeconds(1)) .map(sequence -> ServerSentEvent
	 * .<String>builder() .id(String.valueOf(sequence)) .event("periodic-event")
	 * .data(String.valueOf(getCurrentCount(sessionId))) .build()); }
	 * 
	 * @GetMapping("/stream-int") public Flux<Integer>
	 * intStreamEvents(HttpServletRequest request) {
	 * 
	 * HttpSession session = request.getSession(); String sessionId =
	 * session.getId(); System.out.printf("Session ID: %s\n", sessionId);
	 * System.out.printf("Request thread ID: %d\n", Thread.currentThread().getId());
	 * if (counter.getCounter(sessionId) == -1) { counter.addCounter(sessionId); }
	 * System.out.printf("New counter value is: %d\n", getCurrentCount(sessionId));
	 * 
	 * return Flux.generate(sink -> { ServerSentEvent .<String>builder()
	 * .id(String.valueOf(getCurrentCount(sessionId))) .event("periodic-event")
	 * .data(String.valueOf(getCurrentCount(sessionId))) .build(); }); // return
	 * Flux.generate(sink -> { // sink.next(getCurrentCount(sessionId)); // });
	 * 
	 * }
	 */

	@GetMapping(path = "/counter", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<?> counter(HttpServletRequest request) {

		HttpSession session = request.getSession(false);
		if (session == null) {
			throw new RuntimeException("No Session found for counter");
		}
		String sessionId = session.getId();
		System.out.printf("Session ID: %s\n", sessionId);
		
		CustomSessionData sessionData = (CustomSessionData) session.getAttribute(SESSION_DATA_KEY);
		if (sessionData != null && sessionData.getCurrentDomain() != null) {
			System.out.printf("Current domain: %s", sessionData.getCurrentDomain());
			
			return new ResponseEntity<>(new RestResponseMap<CounterState>().mapOK(counter.getCounter(sessionData.getCurrentDomain())), HttpStatus.OK);
		} else {
			Map<String, String> map = new HashMap<String, String>();
			map.put("errorMessage", "No current session data found");
			return new ResponseEntity<>(simpleMapResponseMap.mapError(map), HttpStatus.BAD_REQUEST);
		}
		
//		System.out.printf("Request thread ID: %d\n", Thread.currentThread().getId());
//		if (counter.getCounter(sessionId) == -1) {
//			counter.addCounter(sessionId);
//		}
//		System.out.printf("New counter value is: %d\n", getCurrentCount(sessionId));
//		return ResponseEntity.of(Optional.of(String.format("{ \"count\": %d }", counter.getCounter(sessionId))));
	}

	
	@RequestMapping(value = "/search", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> search(@RequestBody @Valid SearchRequest searchRequest) {
		try {
			SearchResult result = indexer.search(searchRequest);
			return new ResponseEntity<>(searchResultResponseMap.mapOK(result), HttpStatus.OK);
		} catch (Exception e) {
			log.error("Error to search" + searchRequest.getSearchStr(), e);
			return new ResponseEntity<>(simpleResponseMap.mapError(e.getMessage()), HttpStatus.BAD_REQUEST);
		}
	}	
	
	
	
	
//	public int getCurrentCount(String key) {
//		return counter.getCounter(key);
//	}

	private Flux<String> chunks1K() {
		return Flux.generate(sink -> {
			StringBuilder sb = new StringBuilder();
			do {
				for (char c : "0123456789".toCharArray()) {
					sb.append(c);
					if (sb.length() + 1 == 1024) {
						sink.next(sb.append("\n").toString());
						return;
					}
				}
			} while (true);
		});
	}

	private boolean isValidURL(String url) throws CrawlException {
		String[] schemes = { "http", "https" };
		UrlValidator urlValidator = new UrlValidator(schemes);
		if (!urlValidator.isValid(url)) {
			throw new CrawlException("URL is not valid: " + url);
		}

		HttpClient httpclient = HttpClientBuilder
				.create()
				.setRedirectStrategy(new LaxRedirectStrategy())
				.setUserAgent(USER_AGENT)
				.build();
		HttpGet request = new HttpGet(url);

		try {
			HttpResponse httpResponse = httpclient.execute(request);
			StatusLine statusLine = httpResponse.getStatusLine();
			int statusCode = statusLine.getStatusCode();
			String phrase = statusLine.toString();

			if (statusCode == HttpStatus.OK.value()) {
			} else {
				throw new CrawlException(phrase);
			}
		} catch (ClientProtocolException e) {
			log.error("ClientProtocolException when validate URL: {}", url, e);
			throw new CrawlException(e.getMessage());
		} catch (IOException e) {
			log.error("IOException when validate URL: {}", url, e);
			throw new CrawlException(e.getMessage());
		}

		return true;
	}

	private String extractDomain(String url) {
		WebURL webURL = new WebURL();
		webURL.setURL(url.toLowerCase());
		return webURL.getDomain();
	}

	private boolean needCrawl(String domain) {
		boolean containsDomain = indexer.getIndexedUrls().containsKey(domain);
		return !containsDomain;
	}

}
