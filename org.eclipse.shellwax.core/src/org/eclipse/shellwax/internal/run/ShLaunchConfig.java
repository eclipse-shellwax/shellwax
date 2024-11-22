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
package org.eclipse.shellwax.internal.run;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;

public class ShLaunchConfig extends LaunchConfigurationDelegate {

	public static final String ID = "org.eclipse.shellwax.shlaunchonfigtype";
	public static final String PROGRAM = "org.eclipse.shellwax.launch.program";
	public static final String ARGUMENTS = "org.eclipse.shellwax.launch.arguments";

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
			throws CoreException {
		ILaunchConfigurationWorkingCopy wc = configuration.getWorkingCopy();
		List<String> command = new ArrayList<>();
		String shellPath = wc.getAttribute(PROGRAM, "");
		String[] shellParams = wc.getAttribute(ARGUMENTS, "").split(" ");
		String workDir = wc.getAttribute(DebugPlugin.ATTR_WORKING_DIRECTORY, "");
		if (workDir.isEmpty()) {
			IPath path = new Path(shellPath);
			path = path.removeLastSegments(1);
			workDir = path.toPortableString();
		}
		String executable = "sh";
        if (Platform.OS.isWindows()) {
			executable = "bash.exe";
			String drive = Character.toString(shellPath.charAt(0));
			shellPath = "/mnt/"+drive.toLowerCase()+"/"+shellPath.substring(2);
		}
		command.add(executable);
		command.add(shellPath);
        command.addAll(List.of(shellParams));
		try {
			ProcessBuilder pb = new ProcessBuilder(command.toArray(String[]::new));
			pb.directory(new File(workDir));
			final Process p = pb.start();
			DebugPlugin.newProcess(launch, p, shellPath);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

}
