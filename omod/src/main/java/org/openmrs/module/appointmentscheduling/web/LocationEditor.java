/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.appointmentscheduling.web;

import java.beans.PropertyEditorSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Location;
import org.openmrs.api.context.Context;
import org.springframework.util.StringUtils;

/**
 * Allows for serializing/deserializing a location to a string so that Spring knows how to pass a
 * location back and forth through an html form or other medium.
 */
public class LocationEditor extends PropertyEditorSupport {

	private Log log = LogFactory.getLog(this.getClass());

	public LocationEditor() {
	}

	/**
	 * <strong>Should</strong> set using id
	 * <strong>Should</strong> set using uuid
	 */
	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (StringUtils.hasText(text)) {
			try {
				setValue(Context.getLocationService().getLocation(Integer.valueOf(text)));
			}
			catch (Exception ex) {
				Location location = Context.getLocationService().getLocationByUuid(text);
				setValue(location);
				if (location == null) {
					log.error("Error setting location with id or uuid: " + text, ex);
					throw new IllegalArgumentException("Location not found: " + ex.getMessage());
				}
			}
		}
		else {
			setValue(null);
		}
	}

	@Override
	public String getAsText() {
		Location location = (Location) getValue();
		if (location == null) {
			return "";
		}
		else {
			return location.getId().toString();
		}
	}
}