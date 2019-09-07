package org.thevlad.pwc.index;

import java.util.List;

public class SearchResult {

	private SearchRequest search;
	private List<SearchResultItem> items;

	public SearchResult() {
	}

	public SearchResult(SearchRequest search, List<SearchResultItem> items) {
		this.search = search;
		this.items = items;
	}

	public SearchRequest getSearch() {
		return search;
	}

	public void setSearch(SearchRequest search) {
		this.search = search;
	}

	public List<SearchResultItem> getItems() {
		return items;
	}

	public void setItems(List<SearchResultItem> items) {
		this.items = items;
	}

}
