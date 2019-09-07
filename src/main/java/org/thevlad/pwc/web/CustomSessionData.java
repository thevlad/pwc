package org.thevlad.pwc.web;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CustomSessionData implements Serializable {

	private static final long serialVersionUID = 1L;

	private Map<String,Date> crawlRequests = new HashMap<String, Date>();
	
	private String currentDomain;
	
	public boolean addCrawlRequst(String domain) {
		if (crawlRequests.containsKey(domain)) {
			return false;
		}
		crawlRequests.put(domain, new Date());
		return true;
	}
	
	public boolean isDomainWasSubmitted(String domain) {
		return crawlRequests.containsKey(domain);
	}
	
	public Date getDomainSubmitTimeStamp(String domain) {
		return crawlRequests.get(domain);
	}

	public String getCurrentDomain() {
		return currentDomain;
	}

	public void setCurrentDomain(String currentDomain) {
		this.currentDomain = currentDomain;
	}
	
}
