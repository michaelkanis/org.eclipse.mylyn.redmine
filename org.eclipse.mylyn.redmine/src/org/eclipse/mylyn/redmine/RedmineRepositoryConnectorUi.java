package org.eclipse.mylyn.redmine;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.mylyn.redmine.wizard.QueryWizard;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.ITaskMapping;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.AbstractRepositoryConnectorUi;
import org.eclipse.mylyn.tasks.ui.wizards.ITaskRepositoryPage;

public class RedmineRepositoryConnectorUi extends AbstractRepositoryConnectorUi {

	public RedmineRepositoryConnectorUi() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getConnectorKind() {
		return RedmineRepositoryConnector.CONNECTOR_KIND;
	}

	@Override
	public ITaskRepositoryPage getSettingsPage(TaskRepository taskRepository) {
		return new RedmineRepositorySettingsPage(taskRepository);
	}

	@Override
	public IWizard getQueryWizard(TaskRepository repository,
			IRepositoryQuery queryToEdit) {
		System.out.println("getQueryWizard");
		return new QueryWizard(repository, queryToEdit);
	}

	@Override
	public IWizard getNewTaskWizard(TaskRepository taskRepository,
			ITaskMapping selection) {
		// TODO Auto-generated method stub
		System.out.println("getNewTaskWizard");
		return new Wizard() {
			@Override
			public boolean performFinish() {
				return true;
			}
		};
	}

	@Override
	public boolean hasSearchPage() {
		// TODO Auto-generated method stub
		System.out.println("hasSearchPage");
		return false;
	}

}
