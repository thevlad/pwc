package org.thevlad.pwc.web;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.URL;

public class CrawlRequest {

	@NotEmpty(message="Address cannot be empty!")
	@Pattern(regexp = "^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}$", message = "'${validatedValue}' is not domain name!")
	private String address;

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

}
