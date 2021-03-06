/*******************************************************************************
 * Copyright 2014-2017 Specure GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package at.specure.android.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * map languages that use more than one alphabet to a predefined alphabet
 * @author lb
 *
 */
public class LanguageAlphabetMapperUtil {

	public static final Map<String, String> LANGUAGE_ALPHABET_MAP;
	
	static {
		LANGUAGE_ALPHABET_MAP = new HashMap<String, String>();
		LANGUAGE_ALPHABET_MAP.put("sr", "sr-Latn");
	}
	
	public static String getLanguageString(final Locale locale) {
		if (LANGUAGE_ALPHABET_MAP.containsKey(locale.getLanguage())) {
			return LANGUAGE_ALPHABET_MAP.get(locale.getLanguage());
		}
		
		return locale.getLanguage();
	}
}
