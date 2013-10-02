package org.eclipse.mylyn.redmine.wizard;

import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.wizards.RepositoryQueryWizard;

public class QueryWizard extends RepositoryQueryWizard {

	public QueryWizard(TaskRepository repository, IRepositoryQuery query) {
		super(repository);
		addPage(new IssueRepositoryQueryPage(repository, query));
	}

}
