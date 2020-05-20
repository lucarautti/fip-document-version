package it.fip;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ActionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionStatus;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.docx4j.model.datastorage.migration.VariablePrepare;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("SpellCheckingInspection")
public class CurrentVersion extends ActionExecuterAbstractBase {

	private ServiceRegistry serviceRegistry;	
	private static final Log logger = LogFactory.getLog(CurrentVersion.class);

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
	    this.serviceRegistry = serviceRegistry;
	}
	
	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {

	}

	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
		try {
			this.mainLogic(actionedUponNodeRef);
		} catch (Exception e) {
			e.printStackTrace();
			((ActionImpl)action).setExecutionEndDate(new Date());
			((ActionImpl)action).setExecutionStatus(ActionStatus.Failed);
			((ActionImpl)action).setExecutionFailureMessage("Si è verificato un problema. " + e.getMessage());
			throw new RuntimeException(e);
		}
	}

	private String getUltimaVersione(NodeRef versionableNode) {

		String versione ="";
		VersionHistory versionHistory = serviceRegistry.getVersionService().getVersionHistory(versionableNode);
		if (versionHistory != null) {
			logger.debug("Numero di versioni: " + versionHistory.getAllVersions().size());
            logger.debug("Ultima versione: " + versionHistory.getHeadVersion().getVersionLabel());
            versione = versionHistory.getHeadVersion().getVersionLabel();
		} else {logger.debug("Nodo non versionabile");}
		return versione;
	}

	private void mainLogic(NodeRef nodeRef) throws ContentIOException, Docx4JException {
		try {
            ContentReader reader = this.serviceRegistry.getContentService().getReader(nodeRef, ContentModel.PROP_CONTENT);
            WordprocessingMLPackage originalDocx = WordprocessingMLPackage.load(reader.getContentInputStream());
            VariablePrepare.prepare(originalDocx);
            String versioneAttuale = this.getUltimaVersione(nodeRef);
            HashMap<String, String> mapping = new HashMap<>(1);
                                    mapping.put("versioneAttuale", versioneAttuale);
            originalDocx.getMainDocumentPart().variableReplace(mapping);
            ChildAssociationRef childAssociationRef = this.serviceRegistry.getNodeService().getPrimaryParent(nodeRef);
            NodeRef parent = childAssociationRef.getParentRef();
            String tempDocxName = getDocumentName(nodeRef)+"_temp.docx";
            //ContentWriter writer = this.serviceRegistry.getContentService().getWriter(nodeRef, ContentModel.PROP_CONTENT, true);
            WordprocessingMLPackage tempDocx = originalDocx;
            createContentNode(parent, tempDocxName, tempDocx);
            //originalDocx.save(writer.getContentOutputStream());
        }
		catch (Exception e1) {e1.printStackTrace();}
		
    }

	private String getDocumentName(NodeRef nodeRef) {
		NodeService nodeService = serviceRegistry.getNodeService();
		String nomeIntero = nodeService.getProperty(nodeRef, ContentModel.PROP_NAME).toString();
        String nome = nomeIntero.split("\\.")[0];
		return nome;
	}

    private void createContentNode(NodeRef parent, String name, WordprocessingMLPackage oldDocx )
			throws Docx4JException {
        // Create a map to contain the values of the properties of the node
        Map<QName, Serializable> props = new HashMap<>(1);
        props.put(ContentModel.PROP_NAME, name);

        // use the node service to create a new node
        NodeRef node = this.serviceRegistry.getNodeService().createNode(
                parent,
                ContentModel.ASSOC_CONTAINS,
                QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, name),
                ContentModel.TYPE_CONTENT,
                props).getChildRef();

        // Use the content service to set the content onto the newly created node
        ContentWriter writer = this.serviceRegistry.getContentService().getWriter(node, ContentModel.PROP_CONTENT, true);
        writer.setMimetype(MimetypeMap.MIMETYPE_OPENXML_WORDPROCESSING);
        oldDocx.save(writer.getContentOutputStream());
    }
	/*

	  
	private NodeRef saveWordToPDF(NodeRef parentFolder,String fileName,WordprocessingMLPackage wordMLPackage) {

		NodeRef ref = fileFolderService.create(parentFolder, fileName+".pdf", ContentModel.TYPE_CONTENT).getNodeRef();
		ContentWriter cw = contentService.getWriter(ref, ContentModel.PROP_CONTENT, true);
		cw.setMimetype(MimetypeMap.MIMETYPE_WORD);
		try {
			wordMLPackage.save(cw.getContentOutputStream());
			return convertWordToPDF (ref);
		}
		catch (ContentIOException e) {e.printStackTrace();} 
		catch (Docx4JException e) {e.printStackTrace();} 
		return null;

	}

	private NodeRef convertWordToPDF(NodeRef nodeRef) {

		if (contentService.getReader(nodeRef, ContentModel.PROP_CONTENT) == null) return null;

		try {
			ContentReader cr = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
			ContentWriter cw = contentService.getWriter(nodeRef, ContentModel.PROP_CONTENT, true);
			cw.setMimetype(MimetypeMap.MIMETYPE_PDF);
			if (!cr.getMimetype().equals(MimetypeMap.MIMETYPE_PDF)){
				ContentTransformer ct = contentService.getTransformer(cr.getMimetype(), MimetypeMap.MIMETYPE_PDF);
				if(ct!=null){
					ct.transform(cr, cw);
					return nodeRef;
				}
			}
		}
		catch (FileExistsException e1) {e1.printStackTrace();}
		catch(Exception e) {e.printStackTrace();}
		return null;

	}
	
	

    */

}