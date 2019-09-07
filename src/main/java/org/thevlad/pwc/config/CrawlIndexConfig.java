package org.thevlad.pwc.config;

import org.thevlad.pwc.index.Indexer;

public class CrawlIndexConfig {

	private Indexer indexer;
	
	public CrawlIndexConfig(Indexer indexer) {
		this.indexer = indexer;
	}

	public Indexer getIndexer() {
		return indexer;
	}

	public void setIndexer(Indexer indexer) {
		this.indexer = indexer;
	}

}
