/*
 * Copyright 2015-2018 Emmanuel Keller / QWAZR
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
 */
package com.qwazr.webapps.example;

import com.qwazr.library.annotations.Library;
import com.qwazr.library.asciidoctor.AsciiDoctorTool;
import com.qwazr.library.freemarker.FreeMarkerTool;
import com.qwazr.library.markdown.MarkdownTool;
import com.qwazr.utils.StringUtils;
import freemarker.template.TemplateException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class DocumentationServlet extends HttpServlet {

	protected String[] indexFileNames = { "README.md", "README.adoc" };

	private File documentationPath = new File("src");

	@Library("freemarker")
	private final FreeMarkerTool freemarkerTool = null;

	@Library("markdown")
	private final MarkdownTool markdownTool = null;

	@Library("adoc")
	private final AsciiDoctorTool asciiDoctorTool = null;

	private final MimetypesFileTypeMap mimeTypeMap =
			new MimetypesFileTypeMap(getClass().getResourceAsStream("/com/qwazr/webapps/mime.types"));

	protected String templatePath = "com/qwazr/webapps/example/documentation.ftl";

	@Override
	public void init(ServletConfig config) {
		String p = config.getInitParameter("indexFileNames");
		if (!StringUtils.isEmpty(p))
			indexFileNames = StringUtils.split(p, " ");
		p = config.getInitParameter("documentationPath");
		if (!StringUtils.isEmpty(p))
			documentationPath = new File(p);
		p = config.getInitParameter("templatePath");
		if (!StringUtils.isEmpty(p))
			templatePath = p;
	}

	@Override
	public void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws IOException, ServletException {

		final String remotePrefix = request.getContextPath() + request.getServletPath();
		final String path = request.getPathInfo();
		File file = path == null ? documentationPath : new File(documentationPath, path);

		if (!file.exists()) {
			response.sendError(404, "File not found: " + file);
			return;
		}
		if (file.isDirectory()) {
			if (path == null) {
				response.sendRedirect(remotePrefix + '/');
				return;
			}
			if (!path.endsWith("/")) {
				response.sendRedirect(remotePrefix + path + '/');
				return;
			}
			for (String indexFileName : indexFileNames) {
				File readMefile = new File(file, indexFileName);
				if (readMefile.exists()) {
					file = readMefile;
					break;
				}
			}
		}

		Pair<String, String[]> paths = getRemoteLink(remotePrefix, path);
		request.setAttribute("original_link", paths.getLeft());
		request.setAttribute("breadcrumb_parts", paths.getRight());

		if (!file.exists()) {
			response.sendError(404, "File not found: " + file);
			return;
		}

		request.setAttribute("currentfile", file);
		final String extension = FilenameUtils.getExtension(file.getName());
		final List<File> fileList = getBuildList(file.getParentFile());
		if ("md".equals(extension)) {
			request.setAttribute("markdown", markdownTool.toHtml(file));
			request.setAttribute("filelist", fileList);
		} else if ("adoc".equals(extension)) {
			request.setAttribute("adoc", asciiDoctorTool.convertFile(file));
			request.setAttribute("filelist", fileList);
		} else if (file.isFile()) {
			String type = mimeTypeMap.getContentType(file);
			if (type != null)
				response.setContentType(type);
			response.setContentLengthLong(file.length());
			response.setDateHeader("Last-Modified", file.lastModified());
			response.setHeader("Cache-Control", "max-age=86400");
			response.setDateHeader("Expires", System.currentTimeMillis() + 86400 * 1000);
			try (final InputStream inputStream = new FileInputStream(file)) {
				IOUtils.copy(inputStream, response.getOutputStream());
			}
			return;
		} else if (file.isDirectory()) {
			request.setAttribute("filelist", getBuildList(file));
		} else {
			response.sendRedirect(paths.getLeft());
		}
		try {
			freemarkerTool.template(templatePath, request, response);
		} catch (TemplateException e) {
			throw new ServletException(e);
		}
	}

	protected Pair<String, String[]> getRemoteLink(final String remotePath, final String path) {
		if (StringUtils.isEmpty(path))
			return null;
		final StringBuilder pathBuilder = new StringBuilder(path);
		final String[] parts = StringUtils.split(path, '/');
		if (parts.length > 0) {
			pathBuilder.append(remotePath);
			pathBuilder.append(parts[0]);
			int i = 0;
			for (String part : parts) {
				if (i++ > 0) {
					pathBuilder.append('/');
					pathBuilder.append(part);
				}
			}
		}
		return Pair.of(pathBuilder.toString(), parts);
	}

	protected List<File> getBuildList(File parentFile) {
		final List<File> list = new ArrayList<>();
		final File[] files = parentFile.listFiles();
		if (files == null)
			return list;
		for (final File file : files) {
			if (file.isDirectory()) {
				if (isDocFile(file))
					list.add(file);
			} else if (file.isFile()) {
				if (isDocFile(file.getName()))
					list.add(file);
			}
		}
		return list;
	}

	protected boolean isDocFile(File parentFile) {
		final File[] files = parentFile.listFiles();
		if (files == null)
			return false;
		for (final File file : files) {
			if (file.isFile()) {
				if (isDocFile(file.getName()))
					return true;
			} else if (file.isDirectory()) {
				if (isDocFile(file))
					return true;
			}
		}
		return false;
	}

	protected boolean isDocFile(String fileName) {
		final String extension = FilenameUtils.getExtension(fileName);
		if (extension == null)
			return false;
		return "md".equals(extension) || "adoc".equals(extension);
	}
}
