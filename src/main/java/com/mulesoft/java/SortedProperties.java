package com.mulesoft.java;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

class SortedProperties extends Properties {
	/**
	* 
	*/
	private static final long serialVersionUID = 1L;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Enumeration keys() {

		Enumeration keysEnum = super.keys();
		Vector<String> keyList = new Vector<String>();
		while (keysEnum.hasMoreElements()) {
			keyList.add((String) keysEnum.nextElement());
		}
		Collections.sort(keyList);

		return keyList.elements();
	}

}
