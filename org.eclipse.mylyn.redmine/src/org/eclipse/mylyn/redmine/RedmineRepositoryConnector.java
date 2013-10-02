package org.eclipse.mylyn.redmine;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.commons.net.AuthenticationCredentials;
import org.eclipse.mylyn.commons.net.AuthenticationType;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskDataHandler;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskDataCollector;
import org.eclipse.mylyn.tasks.core.data.TaskMapper;
import org.eclipse.mylyn.tasks.core.sync.ISynchronizationSession;
import org.redmine.ui.Activator;

import com.taskadapter.redmineapi.RedmineException;
import com.taskadapter.redmineapi.RedmineManager;
import com.taskadapter.redmineapi.RedmineSecurityException;
import com.taskadapter.redmineapi.bean.Issue;
import com.taskadapter.redmineapi.bean.Project;

public class RedmineRepositoryConnector extends AbstractRepositoryConnector {

	public static final String CONNECTOR_KIND = "redmine";
	
	private AbstractTaskDataHandler taskDataHandler = new RedmineTaskDataHandler();

	public RedmineRepositoryConnector() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean canCreateNewTask(TaskRepository repository) {
		return false;
	}

	@Override
	public boolean canCreateTaskFromKey(TaskRepository repository) {
		return false;
	}

	@Override
	public String getConnectorKind() {
		return CONNECTOR_KIND;
	}

	@Override
	public String getLabel() {
		return "Redmine Repository";
	}

	@Override
	public String getRepositoryUrlFromTaskUrl(String taskFullUrl) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TaskData getTaskData(TaskRepository repository, String taskId,
			IProgressMonitor monitor) throws CoreException {
		try {
			return createTaskData(repository, createRedmineManager(repository)
					.getIssueById(Integer.valueOf(taskId)), monitor);
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RedmineException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String getTaskIdFromTaskUrl(String taskFullUrl) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasTaskChanged(TaskRepository repository, ITask task,
			TaskData taskData) {
		Date dataDate = getTaskMapping(taskData).getModificationDate();
		Date taskDate = task.getModificationDate();
		return dataDate == null || !dataDate.equals(taskDate);
	}

	@Override
	public IStatus performQuery(TaskRepository repository,
			IRepositoryQuery query, TaskDataCollector collector,
			ISynchronizationSession session, IProgressMonitor monitor) {
		Map<String, String> parameters = query.getAttributes();

		List<String> nullValue = new ArrayList<String>();
		nullValue.add(null);
		parameters.values().removeAll(nullValue);

		boolean queryContainsCustomFields = false;
		for (String key : parameters.keySet()) {
			if (key.startsWith("cf_")) {
				queryContainsCustomFields = true;
				break;
			}
		}

		List<Issue> issues;
		RedmineManager redmine = createRedmineManager(repository);
		if (queryContainsCustomFields && !parameters.containsKey("project_id")) {
			// Custom field filters only work if also filtering for projects
			issues = new ArrayList<Issue>();
			try {
				for (Project project : redmine.getProjects()) {
					Map<String, String> projectParameters = new HashMap<String, String>(
							parameters);
					projectParameters
							.put("project_id", project.getIdentifier());
					try {
						issues.addAll(redmine.getIssues(projectParameters));
					} catch (RedmineSecurityException e) {
						// TODO We weren't authorized; log and skip
					}
				}
			} catch (RedmineException e) {
				return new Status(Status.ERROR, Activator.PLUGIN_ID,
						"Could not get tasks", e);
			}
		} else {
			try {
				issues = redmine.getIssues(parameters);
			} catch (RedmineException e) {
				return new Status(Status.ERROR, Activator.PLUGIN_ID,
						"Could not get tasks", e);
			}
		}

		for (Issue issue : issues) {
			collector.accept(createTaskData(repository, issue, monitor));
		}

		return Status.OK_STATUS;
	}

	private TaskData createTaskData(TaskRepository repository, Issue issue,
			IProgressMonitor monitor) {
		TaskData taskData = new TaskData(getTaskDataHandler()
				.getAttributeMapper(repository), CONNECTOR_KIND,
				repository.getUrl(), issue.getId().toString());

		TaskMapper mapper = new TaskMapper(taskData, true);
		mapper.setSummary(issue.getSubject());
		mapper.setDescription(issue.getDescription());
		mapper.setPriority(issue.getPriorityText());
		mapper.setStatus(issue.getStatusName());
		mapper.setCreationDate(issue.getCreatedOn());
		mapper.setDueDate(issue.getDueDate());
		mapper.setModificationDate(issue.getUpdatedOn());

		if (issue.getTracker() != null) {
			mapper.setTaskKind(issue.getTracker().getName());
		}

		if (issue.getCategory() != null) {
			mapper.setComponent(issue.getCategory().getName());
		}

		if (issue.getAssignee() != null) {
			mapper.setOwner(issue.getAssignee().getFullName());
		}

		if (issue.getProject() != null) {
			mapper.setProduct(issue.getProject().getName());
		}

		if (issue.getTargetVersion() != null) {
			mapper.setVersion(issue.getTargetVersion().getName());
		}

		if (issue.getAuthor() != null) {
			mapper.setReporter(issue.getAuthor().getFullName());
		}

		return taskData;
	}

	@Override
	public AbstractTaskDataHandler getTaskDataHandler() {
		return taskDataHandler;
	}

	@Override
	public void updateRepositoryConfiguration(TaskRepository repository,
			IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateTaskFromTaskData(TaskRepository repository, ITask task,
			TaskData taskData) {
		if (!taskData.isNew()) {
			task.setUrl(getTaskUrl(repository.getUrl(), task.getTaskId()));
		}
		new TaskMapper(taskData).applyTo(task);
	}

	@Override
	public String getTaskUrl(String repositoryUrl, String taskId) {
		return repositoryUrl + "/issues/" + taskId;
	}

	public static RedmineManager createRedmineManager(TaskRepository repository) {
		RedmineManager manager = new RedmineManager(
				repository.getRepositoryUrl());
		AuthenticationCredentials credentials = repository
				.getCredentials(AuthenticationType.REPOSITORY);
		manager.setLogin(credentials.getUserName());
		manager.setPassword(credentials.getPassword());
		return manager;
	}
}
