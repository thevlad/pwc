package org.thevlad.pwc.index;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;

import org.hibernate.validator.constraints.Length;

public class SearchRequest {

	@NotEmpty(message = "Search cannot be empty!")
	@Length(min = 3, message = "Search string cannot be less then 3 symbols!")
	private String searchStr;
	@Min(value = 1, message = "Page number cannot be less than 1")
	private int page;
	private int total;
	private int pages;
	private boolean onlySubmittedDomain;
	private String domain;

	public String getSearchStr() {
		return searchStr;
	}

	public void setSearchStr(String searchStr) {
		this.searchStr = searchStr;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public int getPages() {
		return pages;
	}

	public void setPages(int pages) {
		this.pages = pages;
	}

	public boolean isOnlySubmittedDomain() {
		return onlySubmittedDomain;
	}

	public void setOnlySubmittedDomain(boolean onlySubmittedDomain) {
		this.onlySubmittedDomain = onlySubmittedDomain;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

}
