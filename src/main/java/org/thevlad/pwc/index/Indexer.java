package org.thevlad.pwc.index;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;

public interface Indexer {

	void init() throws IOException;

	void close() throws IOException;

	void reopen() throws IOException;

	void index(String docUrl, String title, String content, String date) throws IOException;

	SearchResult search(SearchRequest request)
			throws IOException, ParseException, InvalidTokenOffsetsException;

	Map<String, Map<String, Date>> getIndexedUrls();

}