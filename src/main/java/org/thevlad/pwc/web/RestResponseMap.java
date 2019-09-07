package org.thevlad.pwc.web;

import java.util.HashMap;
import java.util.Map;

public class RestResponseMap<T> {

	public Map<String, Object> mapOK(T data) {
		Map<String, Object> modelMap = new HashMap<String, Object>(2);
		modelMap.put("success", true);
		modelMap.put("data", data);
		return modelMap;
	}

	public Map<String, Object> mapError(T data) {

		Map<String, Object> modelMap = new HashMap<String, Object>(2);
		modelMap.put("success", false);
		modelMap.put("data", data);
		return modelMap;
	}

}
