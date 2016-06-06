/**
 * Copyright 2016 Emmanuel Keller / QWAZR
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package com.qwazr.webapps;

import org.slf4j.ext.EventData;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Calendar;


public class WebappLogger extends EventData {

	//date time c-ip s-ip cs-method cs-uri-stem  cs-uri-query sc-status  sc(substatus) cs(bytes)  sc(bytes) time-taken  cs(host)

	final private Calendar calendar;
	final private HttpServletRequest request;
	final private HttpServletResponse response;
	final long timeTaken;

	WebappLogger(final HttpServletRequest request, final HttpServletResponse response, final long timeTaken) {
		this.request = request;
		this.response = response;
		this.timeTaken = timeTaken;
		this.calendar = Calendar.getInstance();
		setEventDateTime(calendar.getTime());
		setEventType("transfer");
		put("c-ip", getCIp());
		put("cs-host", getCsHost());
		put("cs-method", getCsMethod());
		put("cs-uri-query", getCsUriQuery());
		put("cs(User-Agent)", getCsUserAgent());
		put("cs(Username)", getCsUsername());
		put("cs(X-Forwarded-For)", getCsXForwardedFor());
		put("date", getDate());
		put("referer", getReferrer());
		put("sc-status", getScStatus());
		put("s-ip", getSIp());
		put("s-port", getSPort());
		put("time", getTime());
		put("timetaken", getTimeTaken());
	}

	private void span2(final StringBuilder sb, final int value) {
		if (value < 10)
			sb.append('0');
		sb.append(value);
	}

	public final String getDate() {
		final StringBuilder sb = new StringBuilder(calendar.get(Calendar.YEAR));
		sb.append('-');
		span2(sb, calendar.get(Calendar.MONTH) + 1);
		sb.append('-');
		span2(sb, calendar.get(Calendar.DAY_OF_MONTH));
		return sb.toString();
	}

	public final String getTime() {
		final StringBuilder sb = new StringBuilder(calendar.get(Calendar.HOUR_OF_DAY));
		sb.append(':');
		span2(sb, calendar.get(Calendar.MINUTE));
		sb.append(':');
		span2(sb, calendar.get(Calendar.SECOND));
		return sb.toString();
	}

	/**
	 * @return the ip address of the client (c-ip)
	 */
	public final String getCIp() {
		return request.getRemoteAddr();
	}

	/**
	 * @return the ip address of the server (s-ip)
	 */
	public final String getSIp() {
		return request.getLocalAddr();
	}

	/**
	 * @return the port of the server (s-port)
	 */
	public final Integer getSPort() {
		return request.getLocalPort();
	}

	/**
	 * @return the HTTP method (cs-method)
	 */
	public final String getCsMethod() {
		return request.getMethod();
	}

	private final String replaceEmpty(final String value) {
		return value == null || value.isEmpty() ? "-" : value;
	}

	/**
	 * @return the remote username (cs-username)
	 */
	public final String getCsUsername() {
		return replaceEmpty(request.getRemoteUser());
	}

	/**
	 * @return the host name of the client (cs-host)
	 */
	public final String getCsHost() {
		return replaceEmpty(request.getRemoteHost());
	}

	/**
	 * @return the stem portion alone of URI (cs-uri-stem)
	 */
	public final String getCsUriStem() {
		return replaceEmpty(request.getPathInfo());
	}

	/**
	 * @return the query portion alone of URI (cs-uri-query)
	 */
	public final String getCsUriQuery() {
		return replaceEmpty(request.getQueryString());
	}

	/**
	 * @return the status code (sc-status)
	 */
	public final Integer getScStatus() {
		return response.getStatus();
	}

	private String getRequestHeader(final String header) {
		return replaceEmpty(request.getHeader(header));
	}

	/**
	 * @return the User-Agent header from the client request cs(User-Agent)
	 */
	public final String getCsUserAgent() {
		return getRequestHeader("User-Agent");
	}

	/**
	 * @return the X-Forwarded-For header from the client request cs(X-Forwarded-For)
	 */
	public final String getCsXForwardedFor() {
		return getRequestHeader("X-Forwarded-For");
	}

	/**
	 * @return the Referer header from the client request cs(Referer)
	 */
	public final String getReferrer() {
		return getRequestHeader("Referer");
	}

	/**
	 * @return the Time taken for transaction to complete in milliseconds
	 */
	public final Long getTimeTaken() {
		return timeTaken;
	}

}
