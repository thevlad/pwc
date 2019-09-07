package org.thevlad.pwc.config;

public class CrawlCustomData {

	private CrawlIndexConfig crawlIndexConfig;
	private String domain;

	public CrawlCustomData() {
	}

	public CrawlCustomData(CrawlIndexConfig crawlIndexConfig, String domain) {
		this.crawlIndexConfig = crawlIndexConfig;
		this.domain = domain;
	}

	public CrawlIndexConfig getCrawlIndexConfig() {
		return crawlIndexConfig;
	}

	public void setCrawlIndexConfig(CrawlIndexConfig crawlIndexConfig) {
		this.crawlIndexConfig = crawlIndexConfig;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

}
