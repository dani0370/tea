/*******************************************************************************
 *  Copyright (c) 2017 SSI Schaefer IT Solutions GmbH and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *      SSI Schaefer IT Solutions GmbH
 *******************************************************************************/
package org.eclipse.tea.library.build.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Named;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.tea.core.services.TaskProgressTracker;
import org.eclipse.tea.core.services.TaskingLog;
import org.eclipse.tea.library.build.chain.TeaBuildChain;
import org.eclipse.tea.library.build.config.TeaBuildConfig;
import org.eclipse.tea.library.build.model.WorkspaceBuild;
import org.eclipse.tea.library.build.services.TeaBuildElementFactory;
import org.eclipse.tea.library.build.services.TeaBuildVisitor;
import org.eclipse.tea.library.build.services.TeaDependencyWireFactory;

/**
 * Task to build the whole workspace. This will create a {@link TeaBuildChain}
 * for all {@link IProject}s in the workspace. This depends on the presence of
 * the required {@link TeaBuildElementFactory}s,
 * {@link TeaDependencyWireFactory}s and {@link TeaBuildVisitor}s to fully
 * process all elements.
 */
@Named("Build all projects")
public class TaskBuildWorkspace {

	@Execute
	public IStatus build(IEclipseContext taskContext, TaskingLog log, TaskProgressTracker tracker, WorkspaceBuild wb,
			TeaBuildConfig config) throws Exception {
		TeaBuildChain chain = TeaBuildChain.make(taskContext,
				Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects()));

		IStatus result = chain.execute(tracker, config.failureThreshold);

		List<String> buildOrder = new ArrayList<>();

		if (result.getSeverity() > IStatus.WARNING) {
			log.error("Errors during build: " + result);
		} else {
			setWorkspaceBuildeOrder(buildOrder);
		}

		return result;
	}

	/**
	 * After a successful build of the workspace, update the workspace build
	 * order to what we have calculated. This assures that the automatic build
	 * will use the same order and thus have less cycles that it needs to run
	 * through.
	 *
	 * @param buildOrder
	 *            the order to set.
	 */
	private void setWorkspaceBuildeOrder(List<String> buildOrder) throws CoreException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceDescription description = workspace.getDescription();

		description.setBuildOrder(buildOrder.toArray(new String[buildOrder.size()]));
		description.setMaxBuildIterations(3);

		workspace.setDescription(description);
	}

}