package action;

import java.util.Date;
import java.util.List;

import org.alfresco.repo.action.ActionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionStatus;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import docx4j.Docx4jHelper;

public class CurrentVersion extends ActionExecuterAbstractBase {

	private Docx4jHelper docx4jHelper;
	private static Log logger = LogFactory.getLog(CurrentVersion.class);
	

	public void setDocx4jHelper(Docx4jHelper docx4jHelper) {
		this.docx4jHelper = docx4jHelper;
	}
	
	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {

	}

	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
		try {
			NodeRef templateRef = docx4jHelper.getDefaultTemplateRef(actionedUponNodeRef);
			docx4jHelper.exportMetadata(templateRef, actionedUponNodeRef,
					docx4jHelper.getDefaultName(actionedUponNodeRef, templateRef));
		} catch (Exception e) {
			e.printStackTrace();
			((ActionImpl)action).setExecutionEndDate(new Date());
			((ActionImpl)action).setExecutionStatus(ActionStatus.Failed);
			((ActionImpl)action).setExecutionFailureMessage("There seems to be a problem. " + e.getMessage());
			throw new RuntimeException(e);
		}
	}

}