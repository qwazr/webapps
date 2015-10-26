/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
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
package com.qwazr.webapps.transaction;

import com.qwazr.utils.LockUtils;
import com.qwazr.utils.StringUtils;
import org.apache.commons.io.FilenameUtils;

import javax.script.ScriptException;
import javax.tools.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

public class FileClassCompilerLoader implements Closeable, AutoCloseable {

	private volatile URLClassLoader classLoader;

	private final File sourceRootFile;

	private final String sourceRootPrefix;
	private final int sourceRootPrefixLength;
	private final URL[] sourceRootURLs;

	private final Map<String, Long> lastModifiedMap;
	private final LockUtils.ReadWriteLock mapRwl;

	public FileClassCompilerLoader(File sourceRootFile) throws MalformedURLException {
		this.sourceRootFile = sourceRootFile;
		this.sourceRootPrefix = sourceRootFile.getAbsolutePath();
		this.sourceRootURLs = new URL[] { sourceRootFile.toURI().toURL() };
		this.sourceRootPrefixLength = sourceRootPrefix.length();
		lastModifiedMap = new HashMap<String, Long>();
		mapRwl = new LockUtils.ReadWriteLock();

	}

	private synchronized void resetClassLoader(boolean closeOnly) throws IOException {
		if (classLoader != null) {
			classLoader.close();
			classLoader = null;
		}
		if (!closeOnly)
			classLoader = new URLClassLoader(sourceRootURLs);
	}

	public Class<?> loadClass(File sourceFile)
					throws IOException, ScriptException, InterruptedException, ClassNotFoundException {
		String sourcePath = sourceFile.getAbsolutePath();
		if (!sourcePath.startsWith(sourceRootPrefix))
			throw new IOException("The file is not in the source root: " + sourceFile + " / " + sourceRootFile);
		String baseName = sourcePath.substring(sourceRootPrefixLength);
		baseName = FilenameUtils.getBaseName(StringUtils.join(StringUtils.split(baseName, '\\'), '.'));
		long sourceFileLastModified = sourceFile.lastModified();

		mapRwl.r.lock();
		try {
			Long time = lastModifiedMap.get(baseName);
			if (time != null && time == sourceFileLastModified)
				return classLoader.loadClass(baseName);
		} finally {
			mapRwl.r.unlock();
		}

		mapRwl.w.lock();
		try {
			Long time = lastModifiedMap.get(baseName);
			if (time != null && time == sourceFileLastModified)
				return classLoader.loadClass(baseName);
			compile(sourceFile);
			lastModifiedMap.put(baseName, sourceFileLastModified);
			resetClassLoader(false);
			return classLoader.loadClass(baseName);
		} finally {
			mapRwl.w.unlock();
		}
	}

	private void compile(File sourceFile) throws IOException {
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
		try {
			Iterable<? extends JavaFileObject> sourceFiles = fileManager.getJavaFileObjects(sourceFile);
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			JavaCompiler.CompilationTask task = compiler.getTask(pw, fileManager, diagnostics, null, null, sourceFiles);
			if (!task.call()) {
				for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics())
					pw.format("Error on line %d in %s%n%s%n", diagnostic.getLineNumber(),
									diagnostic.getSource().toUri(), diagnostic.getMessage(null));
				pw.flush();
				pw.close();
				sw.close();
				throw new IOException(sw.toString());
			}
		} finally {
			fileManager.close();
		}
	}

	@Override
	public void close() throws IOException {
		resetClassLoader(true);
	}
}
