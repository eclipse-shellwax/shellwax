/*******************************************************************************
 * Copyright (c) 2019, 2024 Red Hat Inc. and others.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.lsp4e.server.ProcessStreamConnectionProvider;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wildwebdeveloper.embedder.node.NodeJSManager;

public class BashLanguageServer extends ProcessStreamConnectionProvider {
	private static final String LS_VERSION = "5.4.0";
	private static final String LOCAL_PATH = "/.local/share/shellwax/"+LS_VERSION;
	private static final String LS_MAIN = "/node_modules/.bin/bash-language-server";
	private static final String LS_MAIN_WIN32 = "/bash-language-server";

	private static boolean alreadyWarned;
	private static CompletableFuture<Void> initializeFuture;

	private static String getLsPath() {
		String lsPath = System.getProperty("user.home") + LOCAL_PATH;
		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			lsPath = lsPath + LS_MAIN_WIN32 + ".cmd";
		} else {
			lsPath = lsPath + LS_MAIN;
		}	
		return lsPath;
	}

	private static boolean isInstalled() {
		File installLocation = new File(getLsPath());
		return installLocation.exists() && installLocation.canExecute();
	}

	public BashLanguageServer() {
		List<String> commands = new ArrayList<>();
		String nodePath = NodeJSManager.getNodeJsLocation().getAbsolutePath();
		if (nodePath == null) {
			nodePath = getExecLocation("node");
		}
		if (nodePath != null) {
			if (!isInstalled()) {
				installLS();
			}
			String lsPath = getLsPath();
			if (Platform.getOS().equals(Platform.OS_WIN32)) {
				commands.add("cmd");
				commands.add("/c");
			} else {
				commands.add(nodePath);
				
			}
			commands.add(lsPath);
			commands.add("start");
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
				String npmPath = NodeJSManager.getNpmLocation().getAbsolutePath();
				if (npmPath == null) {
					npmPath = getExecLocation("npm");
				}
				if (Platform.getOS().equals(Platform.OS_WIN32)) {
					npmPath = npmPath+".cmd";
				}
				if (npmPath != null) {
					List<String> commands = List.of(npmPath, "install","--prefix=.","bash-language-server@"+LS_VERSION);
					ProcessBuilder pb = new ProcessBuilder(commands);
					pb.directory(installLocation);
					try {
						Process ps = pb.start();
						ps.waitFor();
					} catch (IOException | InterruptedException e) {
						e.printStackTrace();
					}
				}
			});
		}
		return initializeFuture;
	}

	private static String getExecLocation(String exec) {
		String res = "/path/to/" + exec;
		String[] command = { "/bin/bash", "-c", "which " + exec };
		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			command = new String[] { "cmd", "/c", "where " + exec };
		}
		BufferedReader reader = null;
		try {
			Process p = Runtime.getRuntime().exec(command);
			reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			res = reader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Try default install path as last resort
		if (res == null && Platform.getOS().equals(Platform.OS_MACOSX)) {
			res = "/usr/local/bin/" + exec;
		}

		if (res != null && Files.exists(Paths.get(res))) {
			return res;
		} else if (!alreadyWarned) {
			warnExecMissing(exec);
			alreadyWarned = true;
		}
		return null;
	}

	private static void warnExecMissing(String exec) {
		Display.getDefault().asyncExec(() ->
			MessageDialog.openWarning(Display.getCurrent().getActiveShell(), "Missing " + exec,
					"Could not find node.js. This will result in editors missing key features.\n"
							+ "Please make sure node.js is installed and that your PATH environement variable contains the location to the `node` executable."));
	}
}
