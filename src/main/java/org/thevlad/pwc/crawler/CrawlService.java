package org.thevlad.pwc.crawler;

import org.springframework.scheduling.annotation.Async;

public interface CrawlService {

	@Async
	void crawl(String url) throws CrawlException;
}
