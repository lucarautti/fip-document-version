package it.fip;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.Serializable;
import java.text.SimpleDateFormat;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.alfresco.repo.action.ActionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionStatus;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.cmr.version.VersionHistory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import org.docx4j.model.datastorage.migration.VariablePrepare;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.wml.ContentAccessor;
import org.docx4j.wml.Text;

public class CurrentVersion extends ActionExecuterAbstractBase {

	private ServiceRegistry serviceRegistry;
	
	private static Log logger = LogFactory.getLog(CurrentVersion.class);
	
	// org.docx4j.wml.ObjectFactory foo = Context.getWmlObjectFactory();

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}
	
	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {

	}

	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
		try {
			
			this.variableReplace(actionedUponNodeRef);
		} catch (Exception e) {
			e.printStackTrace();
			((ActionImpl)action).setExecutionEndDate(new Date());
			((ActionImpl)action).setExecutionStatus(ActionStatus.Failed);
			((ActionImpl)action).setExecutionFailureMessage("Si è verificato un problema. " + e.getMessage());
			throw new RuntimeException(e);
		}
	}

	public String getUltimaVersione(NodeRef versionableNode) {

		String versione ="";
		VersionHistory versionHistory = serviceRegistry.getVersionService().getVersionHistory(versionableNode);
		
		if (versionHistory != null) {
			logger.debug("Numero di versioni: " + versionHistory.getAllVersions().size());
            logger.debug("Ultima versione: " + versionHistory.getRootVersion().getVersionLabel());
            versione = versionHistory.getRootVersion().getVersionLabel();
		} else {logger.debug("Nodo non versionabile");}
		return versione;
	}
	
	
	public String getDataCorrente() {
		
		Date date = new Date();  
	    SimpleDateFormat formatter = new SimpleDateFormat("dd MMMM yyyy");  
	    String data = formatter.format(date);
		return data;
	}
	
	
	public void variableReplace(NodeRef nodeRef) throws ContentIOException, Docx4JException {
		
		ContentReader reader = this.serviceRegistry.getContentService().getReader(nodeRef, ContentModel.PROP_CONTENT);
		
		WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.load(reader.getContentInputStream());
		//MainDocumentPart documentPart = wordMLPackage.getMainDocumentPart();
		//List<Object> texts = getAllElementFromObject(documentPart, Text.class);
		String dataModifica = this.getDataCorrente();
		String versioneAttuale = this.getUltimaVersione(nodeRef);
		java.util.HashMap<String, String> mapping = new java.util.HashMap<String, String>();
		try {
			VariablePrepare.prepare(wordMLPackage);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		mapping.put("versioneAttuale", versioneAttuale);
		mapping.put("dataModifica", dataModifica);
		try {
			wordMLPackage.getMainDocumentPart().variableReplace(mapping);
		} catch (JAXBException | Docx4JException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}   
		ContentWriter writer = this.serviceRegistry.getContentService().getWriter(nodeRef, ContentModel.PROP_CONTENT, true);
		try {
			wordMLPackage.save(writer.getContentOutputStream());
		}
		catch (ContentIOException e) {e.printStackTrace();} 
		catch (Docx4JException e) {e.printStackTrace();}
		
	}
	
/*
	private static List<Object> getAllElementFromObject(Object document, Class<?> toSearch) {
		List<Object> result = new ArrayList<Object>();
		if (document instanceof JAXBElement)
			document = ((JAXBElement<?>) document).getValue();

		if (document.getClass().equals(toSearch))
			result.add(document);
		else if (document instanceof ContentAccessor) {
			List<?> children = ((ContentAccessor) document).getContent();
			for (Object child : children) {
				result.addAll(getAllElementFromObject(child, toSearch));
			}
		}
		return result;
	}
*/
	
	public String getDocumentName(NodeRef nodeRef) {
		NodeService nodeService = serviceRegistry.getNodeService();
		String nodeName = nodeService.getProperty(nodeRef, ContentModel.PROP_NAME).toString();
		return nodeName;
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
	
	
	@SuppressWarnings("el-syntax")
	public void searchAndReplace(List<Object> texts, Map<String, String> values){
		
		for (Object text : texts) {
			Text textElement = (Text) text;
			if (textElement.getValue().startsWith("${")) {
				if (mapping.containsKey(textElement.getValue())) {
					textElement.setValue(mapping.get(textElement.getValue()));
				} else {
					textElement.setValue("");
				}
			}
		}
        
    }
    */

}