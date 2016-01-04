/**
 * Copyright 2015-2016 Emmanuel Keller / QWAZR
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package com.qwazr.webapps.transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PathBind {

    final Pattern pattern;
    final String replacement;

    PathBind(String pattern, String replacement) {
	this.pattern = Pattern.compile(pattern);
	this.replacement = replacement;
    }

    /**
     * Load the matcher map by reading the configuration file
     *
     * @param configuration
     * @return the matcher map
     */
    static List<PathBind> loadMatcherConf(Map<String, String> patternMap, List<PathBind> matchers) {
	if (patternMap == null)
	    return null;
	if (matchers == null)
	    matchers = new ArrayList<PathBind>(patternMap.size());
	for (Map.Entry<String, String> entry : patternMap.entrySet())
	    matchers.add(new PathBind(entry.getKey(), entry.getValue()));
	return matchers;
    }

    static List<PathBind> loadMatchers(Map<String, String> patternMap) {
	List<PathBind> matchers = null;
	if (patternMap != null)
	    matchers = loadMatcherConf(patternMap, matchers);
	return matchers;
    }

    static String findMatchingPath(String requestPath, List<PathBind> pathBinds) {
	if (pathBinds == null)
	    return null;
	for (PathBind pathBind : pathBinds) {
	    Matcher matcher;
	    synchronized (pathBind.pattern) {
		matcher = pathBind.pattern.matcher(requestPath);
	    }
	    if (!matcher.find())
		continue;
	    return matcher.replaceAll(pathBind.replacement);
	}
	return null;
    }
}
