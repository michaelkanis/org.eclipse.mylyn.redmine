/*******************************************************************************
 * Copyright (c) 2011 Red Hat and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     David Green <david.green@tasktop.com> - initial contribution
 *     Christian Trutz <christian.trutz@gmail.com> - initial contribution
 *     Chris Aniszczyk <caniszczyk@gmail.com> - initial contribution
 *******************************************************************************/
package org.eclipse.mylyn.redmine.wizard;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.mylyn.commons.core.ICoreRunnable;
import org.eclipse.mylyn.commons.net.Policy;
import org.eclipse.mylyn.commons.ui.CommonUiUtil;
import org.eclipse.mylyn.redmine.RedmineRepositoryConnector;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUiImages;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositoryQueryPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.PlatformUI;

import com.taskadapter.redmineapi.RedmineException;
import com.taskadapter.redmineapi.RedmineManager;
import com.taskadapter.redmineapi.bean.Project;
import com.taskadapter.redmineapi.bean.User;

/**
 * GitHub issue repository query page class.
 */
public class IssueRepositoryQueryPage extends AbstractRepositoryQueryPage {

	private Text titleText;
	private Button openButton;
	private Button closedButton;
	private Combo projectCombo;
	private Combo assigneeCombo;
	private Text customParametersText;

	private SelectionListener completeListener = new SelectionAdapter() {

		public void widgetSelected(SelectionEvent e) {
			setPageComplete(isPageComplete());
		}

	};

	/**
	 * @param pageName
	 * @param taskRepository
	 * @param query
	 */
	public IssueRepositoryQueryPage(String pageName,
			TaskRepository taskRepository, IRepositoryQuery query) {
		super(pageName, taskRepository, query);
		setDescription("Description");
		setPageComplete(false);
	}

	/**
	 * @param taskRepository
	 * @param query
	 */
	public IssueRepositoryQueryPage(TaskRepository taskRepository,
			IRepositoryQuery query) {
		this("issueQueryPage", taskRepository, query); //$NON-NLS-1$
	}

	private void createOptionsArea(Composite parent) {
		Composite optionsArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(optionsArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(optionsArea);

		Composite statusArea = new Composite(optionsArea, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(4).equalWidth(false)
				.applyTo(statusArea);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1)
				.applyTo(statusArea);

		new Label(statusArea, SWT.NONE).setText("Status:");

		openButton = new Button(statusArea, SWT.CHECK);
		openButton.setSelection(true);
		openButton.setText("open");
		openButton.addSelectionListener(completeListener);

		closedButton = new Button(statusArea, SWT.CHECK);
		closedButton.setSelection(true);
		closedButton.setText("closed");
		closedButton.addSelectionListener(completeListener);

		ToolBar toolbar = new ToolBar(statusArea, SWT.FLAT);
		ToolItem updateItem = new ToolItem(toolbar, SWT.PUSH);
		final Image updateImage = TasksUiImages.REPOSITORY_UPDATE_CONFIGURATION
				.createImage();
		toolbar.addDisposeListener(new DisposeListener() {

			public void widgetDisposed(DisposeEvent e) {
				updateImage.dispose();
			}
		});
		updateItem.setImage(updateImage);
		updateItem.setToolTipText("Update Repository");
		GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL)
				.grab(true, false).applyTo(toolbar);
		updateItem.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				refreshRepository();
			}

		});

		Label projectLabel = new Label(optionsArea, SWT.NONE);
		projectLabel.setText("Project");

		projectCombo = new Combo(optionsArea, SWT.DROP_DOWN | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(projectCombo);

		Label assigneeLabel = new Label(optionsArea, SWT.NONE);
		assigneeLabel.setText("Assignee");

		assigneeCombo = new Combo(optionsArea, SWT.DROP_DOWN | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(assigneeCombo);

		Label customParametersLabel = new Label(optionsArea, SWT.NONE);
		customParametersLabel.setText("Custom Fields");

		customParametersText = new Text(optionsArea, SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false)
				.applyTo(customParametersText);

		// Label assigneeLabel = new Label(optionsArea, SWT.NONE);
		// assigneeLabel.setText("Assignee");
		//
		// assigneeText = new Text(optionsArea, SWT.BORDER | SWT.SINGLE);
		// GridDataFactory.fillDefaults().grab(true,
		// false).applyTo(assigneeText);
		//
		// Label mentionLabel = new Label(optionsArea, SWT.NONE);
		// mentionLabel.setText("Mention");
		//
		// mentionText = new Text(optionsArea, SWT.BORDER | SWT.SINGLE);
		// GridDataFactory.fillDefaults().grab(true,
		// false).applyTo(mentionText);
	}

	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite displayArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(true)
				.applyTo(displayArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(displayArea);

		if (!inSearchContainer()) {
			Composite titleArea = new Composite(displayArea, SWT.NONE);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(titleArea);
			GridDataFactory.fillDefaults().grab(true, false).span(2, 1)
					.applyTo(titleArea);

			new Label(titleArea, SWT.NONE).setText("Title");
			titleText = new Text(titleArea, SWT.SINGLE | SWT.BORDER);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(titleText);
			titleText.addModifyListener(new ModifyListener() {

				public void modifyText(ModifyEvent e) {
					setPageComplete(isPageComplete());
				}
			});
		}

		createOptionsArea(displayArea);

		initialize();
		setControl(displayArea);
	}

	private void initialize() {
		IRepositoryQuery query = getQuery();
		if (query == null) {
			return;
		}

		// String milestoneNumber = query
		// .getAttribute(IssueService.FILTER_MILESTONE);
		// if (milestoneNumber != null && milestones != null) {
		// int index = 0;
		// for (Milestone milestone : milestones) {
		// index++;
		// if (milestoneNumber.equals(Integer.toString(milestone
		// .getNumber()))) {
		// milestoneCombo.select(index);
		// break;
		// }
		// }
		// }
		//
		// titleText.setText(query.getSummary());
		// labelsViewer.setCheckedElements(QueryUtils.getAttributes(
		// IssueService.FILTER_LABELS, query).toArray());
		// List<String> status = QueryUtils.getAttributes(
		// IssueService.FILTER_STATE, query);
		// closedButton.setSelection(status.contains(IssueService.STATE_CLOSED));
		// openButton.setSelection(status.contains(IssueService.STATE_OPEN));
		//
	}

	private List<User> users;
	
	private List<Project> projects;

	private void refreshRepository() {
		try {
			ICoreRunnable runnable = new ICoreRunnable() {

				public void run(IProgressMonitor monitor) throws CoreException {
					Policy.monitorFor(monitor);
					monitor.beginTask("", 2);

					monitor.setTaskName("Getting users …");
					refreshProjects();
					monitor.worked(1);
					refreshUsers();
					monitor.done();

					PlatformUI.getWorkbench().getDisplay()
							.asyncExec(new Runnable() {
								public void run() {
									updateProjects();
									updateUsers();
									// initialize();
								}
							});
				}
			};
			IRunnableContext context = getContainer();
			if (context == null)
				if (inSearchContainer())
					context = getSearchContainer().getRunnableContext();
				else
					context = PlatformUI.getWorkbench().getProgressService();
			CommonUiUtil.run(context, runnable);
		} catch (CoreException e) {
			IStatus status = e.getStatus();
			ErrorDialog.openError(getShell(), "Error", e.getLocalizedMessage(),
					status);
		}
	}

	private void refreshProjects() {
		TaskRepository repository = getTaskRepository();
		RedmineManager manager = RedmineRepositoryConnector
				.createRedmineManager(repository);
		try {
			projects = manager.getProjects();
		} catch (RedmineException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void refreshUsers() {
		TaskRepository repository = getTaskRepository();
		RedmineManager manager = RedmineRepositoryConnector
				.createRedmineManager(repository);
		try {
			users = manager.getUsers();
		} catch (RedmineException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void updateProjects() {
		if (projectCombo.isDisposed()) {
			return;
		}

		projectCombo.removeAll();
		projectCombo.add("<All>");

		Collections.sort(projects, new Comparator<Project>() {
			@Override
			public int compare(Project o1, Project o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});

		for (Project project : projects) {
			projectCombo.add(project.getName());
		}

		projectCombo.select(0);
	}

	private void updateUsers() {
		if (assigneeCombo.isDisposed()) {
			return;
		}

		assigneeCombo.removeAll();
		assigneeCombo.add("<All>");

		Collections.sort(users, new Comparator<User>() {
			@Override
			public int compare(User o1, User o2) {
				return o1.getFullName().compareTo(o2.getFullName());
			}
		});

		for (User user : users) {
			assigneeCombo.add(user.getFullName());
		}

		assigneeCombo.select(0);
	}

	/**
	 * @see org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositoryQueryPage#isPageComplete()
	 */
	public boolean isPageComplete() {
		boolean complete = inSearchContainer() ? true : super.isPageComplete();
		if (complete) {
			String message = null;
			if (!openButton.getSelection() && !closedButton.getSelection())
				message = "Messages.IssueRepositoryQueryPage_ErrorStatus";

			setErrorMessage(message);
			complete = message == null;
		}
		return complete;
	}

	/**
	 * @see org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositoryQueryPage#getQueryTitle()
	 */
	public String getQueryTitle() {
		return titleText != null ? titleText.getText() : null;
	}

	/**
	 * @see org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositoryQueryPage#applyTo(org.eclipse.mylyn.tasks.core.IRepositoryQuery)
	 */
	public void applyTo(IRepositoryQuery query) {
		query.setSummary(getQueryTitle());

		// List<String> statuses = new LinkedList<String>();
		// if (openButton.getSelection())
		// statuses.add(IssueService.STATE_OPEN);
		// if (closedButton.getSelection())
		// statuses.add(IssueService.STATE_CLOSED);
		// QueryUtils.setAttribute(IssueService.FILTER_STATE, statuses, query);

		if (openButton.getSelection() && closedButton.getSelection()) {
			query.setAttribute("status_id", "*");
		} else if (openButton.getSelection()) {
			query.setAttribute("status_id", "open");
		} else if (closedButton.getSelection()) {
			query.setAttribute("status_id", "closed");
		} else {
			// Should not happen
		}

		//
		// String assignee = assigneeText.getText().trim();
		// if (assignee.length() > 0) {
		// query.setAttribute(IssueService.FILTER_ASSIGNEE, assignee);
		// } else {
		// query.setAttribute(IssueService.FILTER_ASSIGNEE, null);
		// }
		//
		// String mentions = mentionText.getText().trim();
		// if (mentions.length() > 0)
		// query.setAttribute(IssueService.FILTER_MENTIONED, mentions);
		// else
		// query.setAttribute(IssueService.FILTER_MENTIONED, null);
		//
		int assigneeSelected = assigneeCombo.getSelectionIndex() - 1;
		if (assigneeSelected >= 0) {
			query.setAttribute("assigned_to_id",
					Integer.toString(users.get(assigneeSelected).getId()));
		} else {
			query.setAttribute("assigned_to_id", null);
		}

		if (!customParametersText.getText().trim().equals("")) {
			String customFields = customParametersText.getText().trim();
			String[] params;
			if (customFields.indexOf(',') != -1) {
				params = customFields.split(",");
			} else {
				params = new String[] { customFields };
			}
			for (String param : params) {
				String[] keyValue = param.trim().split("=");
				if (keyValue.length != 2) {
					continue;
				}
				query.setAttribute(keyValue[0].trim(), keyValue[1].trim());
			}
		}

		//
		// List<String> labels = new LinkedList<String>();
		// for (Object label : labelsViewer.getCheckedElements())
		// labels.add(label.toString());
		// QueryUtils.setAttribute(IssueService.FILTER_LABELS, labels, query);
	}
}
