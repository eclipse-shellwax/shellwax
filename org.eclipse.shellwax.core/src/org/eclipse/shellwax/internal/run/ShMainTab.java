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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class ShMainTab extends AbstractLaunchConfigurationTab {

	private Text programPathText;
	private Text argumentsText;
	private Text workingDirectoryText;

	@Override
	public void createControl(Composite parent) {
		Composite resComposite = new Composite(parent, SWT.NONE);
		resComposite.setLayout(new GridLayout(2, false));
		new Label(resComposite, SWT.NONE).setText("Shell script");
		this.programPathText = new Text(resComposite, SWT.BORDER);
		this.programPathText.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));
		ControlDecoration decoration = new ControlDecoration(programPathText, SWT.TOP | SWT.LEFT);
		FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR);
		decoration.setImage(fieldDecoration.getImage());
		this.programPathText.addModifyListener(_ -> {
			setDirty(true);
			File file = new File(programPathText.getText());
			if (!file.isFile()) {
				String errorMessage = "Given path doesn't reference a file.";
				setErrorMessage(errorMessage);
				decoration.setDescriptionText(errorMessage);
				decoration.show();
			} else if (!file.canRead()) {
				String errorMessage = "Not allowed to read file.";
				setErrorMessage(errorMessage);
				decoration.setDescriptionText(errorMessage);
				decoration.show();
			} else {
				setErrorMessage(null);
				decoration.hide();
			}
			updateLaunchConfigurationDialog();
		});
		new Label(resComposite, SWT.NONE).setText("Arguments");
		this.argumentsText = new Text(resComposite, SWT.BORDER);
		this.argumentsText.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));
		this.argumentsText.addModifyListener(_ -> {
			setDirty(true);
			updateLaunchConfigurationDialog();
		});
		new Label(resComposite, SWT.NONE).setText("Working directory");
		this.workingDirectoryText = new Text(resComposite, SWT.BORDER);
		this.workingDirectoryText.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));
		this.workingDirectoryText.addModifyListener(_ -> {
			setDirty(true);
			updateLaunchConfigurationDialog();
		});
		setControl(resComposite);
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		// Nothing to do
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			this.programPathText.setText(configuration.getAttribute(ShLaunchConfig.PROGRAM, "")); //$NON-NLS-1$
			this.argumentsText.setText(configuration.getAttribute(ShLaunchConfig.ARGUMENTS, "")); //$NON-NLS-1$
			this.workingDirectoryText.setText(configuration.getAttribute(DebugPlugin.ATTR_WORKING_DIRECTORY, "")); //$NON-NLS-1$
		} catch (CoreException e) {
            ILog.get().log(e.getStatus());
		}
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(ShLaunchConfig.PROGRAM, this.programPathText.getText());
		configuration.setAttribute(ShLaunchConfig.ARGUMENTS, this.argumentsText.getText());
		configuration.setAttribute(DebugPlugin.ATTR_WORKING_DIRECTORY, this.workingDirectoryText.getText());
	}

	@Override
	public String getName() {
		return "Shell script";
	}

}
