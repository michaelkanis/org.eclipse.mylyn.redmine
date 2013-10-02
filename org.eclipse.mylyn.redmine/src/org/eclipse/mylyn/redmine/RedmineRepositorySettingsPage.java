package org.eclipse.mylyn.redmine;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositorySettingsPage;
import org.eclipse.swt.widgets.Composite;
import org.redmine.ui.Activator;

import com.taskadapter.redmineapi.RedmineException;
import com.taskadapter.redmineapi.RedmineManager;

public class RedmineRepositorySettingsPage extends
		AbstractRepositorySettingsPage {

	public RedmineRepositorySettingsPage(TaskRepository taskRepository) {
		super("Redmine Settings",
				"Supports Redmine 1.3.0+, 2.0+, and ChiliProject",
				taskRepository);
		setNeedsHttpAuth(true);
		setNeedsAdvanced(false);
		setNeedsEncoding(false);
	}

	@Override
	public String getConnectorKind() {
		return RedmineRepositoryConnector.CONNECTOR_KIND;
	}

	@Override
	protected void createAdditionalControls(Composite parent) {
		// do nothing
	}

	@Override
	protected Validator getValidator(final TaskRepository repository) {
		return new Validator() {

			@Override
			public void run(IProgressMonitor monitor) throws CoreException {
				RedmineManager manager = RedmineRepositoryConnector
						.createRedmineManager(repository);

				try {
					System.out.println(manager.getCurrentUser());
				} catch (RedmineException e) {
					setStatus(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
							"Could not connect", e));
				}
			}
		};
	}

}
