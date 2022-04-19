/*******************************************************************************
 * Copyright (c) 2019 Red Hat Inc. and others.
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

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationSelectionDialog;
import org.eclipse.debug.ui.ILaunchShortcut2;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.FileEditorInput;

public class ShLaunchShortcut implements ILaunchShortcut2 {

	@Override
	public void launch(ISelection selection, String mode) {
		ILaunchConfiguration[] configurations = getLaunchConfigurations(selection);
		launch(mode, configurations);
	}

	@Override
	public void launch(IEditorPart editor, String mode) {
		ILaunchConfiguration[] configurations = getLaunchConfigurations(editor);
		launch(mode, configurations);
	}

	@Override
	public ILaunchConfiguration[] getLaunchConfigurations(ISelection selection) {
		return getLaunchConfigurations(getLaunchableResource(selection));
	}

	@Override
	public ILaunchConfiguration[] getLaunchConfigurations(IEditorPart editor) {
		return getLaunchConfigurations(getLaunchableResource(editor));
	}

	@Override
	public IResource getLaunchableResource(ISelection selection) {
		if (selection instanceof IStructuredSelection structuredSelection) {
			if (structuredSelection.size() != 1) {
				return null;
			}
			Object firstObject = structuredSelection.getFirstElement();
			return Adapters.adapt(firstObject, IResource.class);
		}
		return null;
	}

	@Override
	public IResource getLaunchableResource(IEditorPart editor) {
		IEditorInput input = editor.getEditorInput();
		if (input instanceof FileEditorInput file) {
			return file.getFile();
		}
		return null;
	}

	private ILaunchConfiguration[] getLaunchConfigurations(IResource resource) {
		if (resource == null || !resource.isAccessible()) {
			return new ILaunchConfiguration[0];
		}
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType configType = launchManager.getLaunchConfigurationType(ShLaunchConfig.ID);
		try {
			ILaunchConfiguration[] existing = Arrays.stream(launchManager.getLaunchConfigurations(configType))
					.filter(launchConfig -> {
						try {
							return launchConfig.getAttribute(ShLaunchConfig.PROGRAM, "") //$NON-NLS-1$
									.equals(resource.getLocation().toFile().toString());
						} catch (CoreException e) {
							ErrorDialog.openError(Display.getDefault().getActiveShell(), "error", e.getMessage(), e.getStatus()); //$NON-NLS-1$
							return false;
						}
					}).toArray(ILaunchConfiguration[]::new);
			if (existing.length != 0) {
				return existing;
			}
			String configName = launchManager.generateLaunchConfigurationName(resource.toString());
			ILaunchConfigurationWorkingCopy wc = configType.newInstance(null, configName);
			wc.setAttribute(ShLaunchConfig.PROGRAM, resource.getLocation().toString());
			wc.setAttribute(DebugPlugin.ATTR_WORKING_DIRECTORY,
					resource.getLocation().removeLastSegments(1).toString());
			wc.doSave();
			return new ILaunchConfiguration[] { wc };
		} catch (CoreException e) {
			ErrorDialog.openError(Display.getDefault().getActiveShell(), "error", e.getMessage(), e.getStatus()); //$NON-NLS-1$
		}
		return null;
	}

	private void launch(String mode, ILaunchConfiguration[] configurations) {
		if (configurations.length == 1) {
			CompletableFuture.runAsync(() -> {
				try {
					configurations[0].launch(mode, new NullProgressMonitor());
				} catch (CoreException e) {
					ErrorDialog.openError(Display.getDefault().getActiveShell(), "error", e.getMessage(), //$NON-NLS-1$
							e.getStatus());
				}
			});
		} else if (configurations.length > 1) {
			LaunchConfigurationSelectionDialog dialog = new LaunchConfigurationSelectionDialog(
					Display.getDefault().getActiveShell(), configurations);
			if (dialog.open() == IDialogConstants.OK_ID) {
				launch(mode,
						Arrays.asList(dialog.getResult()).toArray(new ILaunchConfiguration[dialog.getResult().length]));
			}
		}
	}

}
