package org.thevlad.pwc.crawler;

import java.io.IOException;
import java.time.LocalDate;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thevlad.pwc.index.Indexer;
import org.thevlad.pwc.services.CounterService;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

public class Crawler extends WebCrawler {

	static final Logger logger = LoggerFactory.getLogger(Crawler.class);
	private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|gif|jpg|png|mp3|mp4|zip|gz|woff|woff2))$");

	private Indexer indexer;
	private String domain;
	private CounterService counterService;

	public Indexer getIndexer() {
		return indexer;
	}

	public void setIndexer(Indexer indexer) {
		this.indexer = indexer;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public CounterService getCounterService() {
		return counterService;
	}

	public void setCounterService(CounterService counterService) {
		this.counterService = counterService;
	}

	@Override
	public boolean shouldVisit(Page page, WebURL url) {
		String href = url.getURL().toLowerCase();
		boolean indexed = indexer.getIndexedUrls().containsKey(href);
		if (indexed) {
			logger.debug("Crawler:: Page already indexed for URL: {}", url.toString());
		}
		boolean matchFilters = FILTERS.matcher(href).matches();
		String subDomain = url.getSubDomain();
		String domainToVisit = url.getDomain();
		boolean matchDomain = (domain == null ? true : domain.equals(domainToVisit));
		
		return (matchDomain && !matchFilters && !indexed);
	}

	@Override
	public void visit(Page page) {
		String url = page.getWebURL().getURL();
		logger.debug("Visit URL: " + url);

		if (page.getParseData() instanceof HtmlParseData) {
			HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
//			String text = htmlParseData.getText();
			String originHtml = htmlParseData.getHtml();
			LocalDate date = null;
			String dateStr = htmlParseData.getMetaTagValue("last-modified");
			if (StringUtils.isEmpty(dateStr)) {
				Header[] headers = page.getFetchResponseHeaders();
				for (Header header : headers) {
					if (header.getName().equalsIgnoreCase("date")) {
						dateStr = header.getValue();
						break;
					}
				}
			}
			
			// breaks multi-level of escaping, preventing &amp;lt;script&amp;gt; to be rendered as <script>
            String _docText = Jsoup.parse(originHtml).text();
			String replace = _docText.replace("&amp;", "");
			// decode any encoded html, preventing &lt;script&gt; to be rendered as <script>
			String html = StringEscapeUtils.unescapeHtml(replace);
			// remove all html tags, but maintain line breaks
			String docText = Jsoup.clean(html, "", Whitelist.none(), new Document.OutputSettings().prettyPrint(false));
			// decode html again to convert character entities back into text
//			String docText =  StringEscapeUtils.unescapeHtml(clean);			
			
			String title = htmlParseData.getTitle();
			logger.debug("title: {}", title);
//			Set<WebURL> links = htmlParseData.getOutgoingUrls();

			try {
				indexer.index(url.toLowerCase(), title, docText, dateStr);
				counterService.increment(domain);
			} catch (IOException e) {
				logger.error("Error to index page: " + url, e);
			}
		}
	}

}
