/*******************************************************************************
 * Copyright (c) 2019, 2025 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Alexander Kurtakov (Red Hat Inc.)- initial implementation
 *******************************************************************************/
package org.eclipse.shellwax.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.Platform;
import org.eclipse.lsp4e.server.ProcessStreamConnectionProvider;
import org.eclipse.wildwebdeveloper.embedder.node.NodeJSManager;

public class BashLanguageServer extends ProcessStreamConnectionProvider {
	private static final String LS_VERSION = "5.4.3";
	private static final String LOCAL_PATH = "/.local/share/shellwax/"+LS_VERSION;
	private static final String LS_MAIN = "/node_modules/.bin/bash-language-server";

	private static CompletableFuture<Void> initializeFuture;

	private static String getLsPath() {
		String lsPath = System.getProperty("user.home") + LOCAL_PATH + LS_MAIN;
		if (Platform.OS.isWindows())
			lsPath = lsPath.replace('/', '\\');
		return lsPath;
	}

	private static boolean isInstalled() {
		File installLocation = new File(getLsPath());
		return installLocation.exists() && installLocation.canExecute();
	}

	public BashLanguageServer() {
		List<String> commands = new ArrayList<>();
		String nodePath = NodeJSManager.getNodeJsLocation().getAbsolutePath();
		
		if (nodePath != null) {
			if (!isInstalled()) {
				installLS();
			}
			String lsPath = getLsPath();
			if (Platform.OS.isWindows()) {
				commands.add("cmd");
				commands.add("/c");
				// quoting lsPath to support spaces in username
				commands.add("\"\"" + lsPath + "\" start\"");
			} else {
				commands.add(nodePath);
				commands.add(lsPath);
				commands.add("start");
			}
			setCommands(commands);
			setWorkingDirectory(System.getProperty("user.dir"));
		}
	}

	@Override
	public void start() throws IOException {
		if (!isInstalled()) {
			installLS().join();
		}
		super.start();
	}

	/**
	 * Creates and asynchronously runs a runnable for the Bash Language Server module
	 * installation if it's not yet created. Returns the CompletableFuture object to follow the
	 * installation runnable that allows at least to wait for the finishing of the installation.
	 *
	 * @return CompletableFuture for the installation  runnable
	 */
	private synchronized CompletableFuture<Void> installLS() {
		if (initializeFuture == null) {
			initializeFuture = CompletableFuture.runAsync(() -> {
				File installLocation = new File(System.getProperty("user.home") + LOCAL_PATH);
				if (!installLocation.isDirectory())
					installLocation.delete();
				if (!installLocation.exists()) {
					installLocation.mkdirs();
					File nodeModulesDir = new File(installLocation, "node_modules");
					nodeModulesDir.mkdir();
				}
				ProcessBuilder pb = NodeJSManager.prepareNPMProcessBuilder("install","--prefix=.","bash-language-server@"+LS_VERSION);
				pb.directory(installLocation);
				pb.inheritIO(); // Redirects stdout and stderr to System.out
				try {
					Process ps = pb.start();
					ps.waitFor();
				} catch (IOException | InterruptedException e) {
					e.printStackTrace();
				}
			});
		}
		return initializeFuture;
	}
}
