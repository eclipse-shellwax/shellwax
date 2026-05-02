/*******************************************************************************
 * Copyright (c) 2026 Aleksandar Kurtakov and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.shellwax.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class ShebangDecorator extends BaseLabelProvider implements ILabelDecorator {

	private static final String SHELL_ICON_KEY = "org.eclipse.shellwax.sh_icon"; //$NON-NLS-1$
	private static final String PLUGIN_ID = "org.eclipse.shellwax.core"; //$NON-NLS-1$
	private static final String SHEBANG_CONTENT_TYPE = "org.eclipse.shellwax.shebang"; //$NON-NLS-1$

	@Override
	public Image decorateImage(Image image, Object element) {
		if (!(element instanceof IFile file)) {
			return null;
		}
		try {
			IContentDescription desc = file.getContentDescription();
			if (desc != null && desc.getContentType() != null) {
				IContentType shebangType = Platform.getContentTypeManager().getContentType(SHEBANG_CONTENT_TYPE);
				if (shebangType != null && desc.getContentType().isKindOf(shebangType)) {
					return getShellImage();
				}
				// Content type was positively identified as something else
				return null;
			}
			// desc is null: Eclipse could not determine the content type from the file
			// name alone (e.g. extension-less files). Fall back to reading the first line.
			if (hasShellShebang(file)) {
				return getShellImage();
			}
		} catch (CoreException | IOException e) {
			// File may not be accessible; leave it with the default icon
		}
		return null;
	}

	private static boolean hasShellShebang(IFile file) throws CoreException, IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getContents(), StandardCharsets.UTF_8))) {
			String line = reader.readLine();
			return line != null && line.startsWith("#!") //$NON-NLS-1$
					&& (line.contains("bash") || line.contains("bin/sh")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	@Override
	public String decorateText(String text, Object element) {
		return null;
	}

	private static Image getShellImage() {
		Image img = JFaceResources.getImageRegistry().get(SHELL_ICON_KEY);
		if (img == null) {
			ImageDescriptor desc = AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, "icons/sh.png"); //$NON-NLS-1$
			if (desc != null) {
				JFaceResources.getImageRegistry().put(SHELL_ICON_KEY, desc);
				img = JFaceResources.getImageRegistry().get(SHELL_ICON_KEY);
			}
		}
		return img;
	}
}
