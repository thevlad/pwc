package org.thevlad.pwc.web;

import java.util.List;

import org.thevlad.pwc.index.SearchResult;

public class SearchResponse {

	private int total;
	private int page;
	private List<SearchResult> results;

	public SearchResponse(int total, int page, List<SearchResult> results) {
		super();
		this.total = total;
		this.page = page;
		this.results = results;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public List<SearchResult> getResults() {
		return results;
	}

	public void setResults(List<SearchResult> results) {
		this.results = results;
	}

}
