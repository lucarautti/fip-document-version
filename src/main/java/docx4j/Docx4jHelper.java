package docx4j;

import java.io.Serializable;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.transform.ContentTransformer;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.docx4j.XmlUtils;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.PartName;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.wml.ContentAccessor;
import org.docx4j.wml.Text;

public class Docx4jHelper {

	
	private ServiceRegistry serviceRegistry;
	private static Log logger = LogFactory.getLog(Docx4jHelper.class);
	
	org.docx4j.wml.ObjectFactory foo = Context.getWmlObjectFactory();


	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
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
	
	
	public void variableReplace(NodeRef nodeRef) {
		
		ContentReader reader = this.serviceRegistry.getContentService().getReader(nodeRef, ContentModel.PROP_CONTENT);
		WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.load(reader.getContentInputStream());
		MainDocumentPart documentPart = wordMLPackage.getMainDocumentPart();
		org.docx4j.model.datastorage.migration.VariablePrepare.prepare(wordMLPackage);
		HashMap<String, String> mappings = new HashMap<String, String>();
		mappings.put("versioneAttuale", (this.getUltimaVersione(nodeRef)));
		mappings.put("dataModifica", (this.getDataCorrente()));
		documentPart.variableReplace(mappings);
		if (save) {
			SaveToZipFile saver = new SaveToZipFile(wordMLPackage);
			saver.save(outputfilepath);
		} else {
			System.out.println(XmlUtils.marshaltoString(documentPart.getJaxbElement(), true,
					true));
		}
		
	}
	

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

	
	public String getDefaultName(NodeRef nodeRef) {
		NodeService nodeService = serviceRegistry.getNodeService();
		String nodeName = nodeService.getProperty(nodeRef, ContentModel.PROP_NAME).toString();
		return nodeName;
	}


	
	private Map<String, String> getMetadata(NodeRef nodeRef, Boolean includeBruckets) {
		HashMap<String, String> mapping = new HashMap<String, String>();
		NodeService nodeService = serviceRegistry.getNodeService();
		Map<QName, Serializable> properties = nodeService.getProperties(nodeRef);
		for (QName property : properties.keySet()) {
			String variableName = includeBruckets ? "${" + property.getLocalName() + "}" : property.getLocalName();
			String value = properties.get(property) == null ? "" : properties.get(property).toString();
			mapping.put(variableName, value);
		}
		return mapping;
	}

	public NodeRef getTemplateInstance(NodeRef templateRef, NodeRef targetRef, String name) {
		NodeRef instance = this.serviceRegistry.getCopyService().copy(templateRef, targetRef,
				org.alfresco.model.ContentModel.ASSOC_CONTAINS, org.alfresco.model.ContentModel.ASSOC_ORIGINAL);
		this.serviceRegistry.getNodeService().setProperty(instance, org.alfresco.model.ContentModel.PROP_NAME, name);
		return instance;
	}

	/**
	 * Substitute mapping variables into the docx4j document.
	 * 
	 * @param nodeRef.  NodeRef of the docx4j document
	 * @param mappings. Map of name-value of all variables to substitute
	 * @throws Docx4JException
	 * @throws ContentIOException
	 */
	@SuppressWarnings("deprecation")
	public void printWordDocumentByMapping(NodeRef nodeRef, Map<String, String> mapping) throws ContentIOException, Docx4JException {
		
		ContentReader reader = this.serviceRegistry.getContentService().getReader(nodeRef, ContentModel.PROP_CONTENT);
		WordprocessingMLPackage document = WordprocessingMLPackage.load(reader.getContentInputStream());
		List<Object> texts = getAllElementFromObject(document.getMainDocumentPart(), Text.class);
		texts.addAll(getAllElementFromObject(document.getHeaderFooterPolicy().getDefaultHeader(), Text.class));
		replacePlaceholder(texts, mapping);

		ContentWriter writer = this.serviceRegistry.getContentService().getWriter(nodeRef, ContentModel.PROP_CONTENT,
				true);
		document.save(writer.getContentOutputStream());
	}

	/*
	 
	private void replacePlaceholder(List<Object> texts, Map<String, String> mapping) {
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
	
	private void replacePlaceholder(WordprocessingMLPackage template,String name, String placeholder) {
		
		List<Object> texts = getAllElementFromObject(template.getMainDocumentPart(), Text.class);
		for (Object text : texts) {
			Text textElement = (Text) text;
			if (textElement.getValue().equals(placeholder)) {
				textElement.setValue(name);
            }
        }
    }
	
	
	/*
	 * Questo metodo effettua il merge di più documenti Word
	 * 
	 
	
	private WordprocessingMLPackage mergeDocs(NodeRef parentFolder,List<String> fileList) 
			throws Docx4JException, URISyntaxException { 
		
		WordprocessingMLPackage docDest=null;
		NodeRef ref = nodeService.getChildByName(productFolder, ContentModel.ASSOC_CONTAINS, fileList.get(0));
		if(ref==null) return null;
		ContentReader reader=contentService.getReader(ref, ContentModel.PROP_CONTENT);
		if(reader==null) return null;
		docDest = WordprocessingMLPackage.load(reader.getContentInputStream());
		if (fileList.size() == 1) return docDest;
		else {
			for(int i=1;i<fileList.size();i++){
				NodeRef ref2=nodeService.getChildByName(productFolder, ContentModel.ASSOC_CONTAINS, fileList.get(i));
				ContentReader reader2=contentService.getReader(ref2, ContentModel.PROP_CONTENT);
				WordprocessingMLPackage docSource = WordprocessingMLPackage.load(reader2.getContentInputStream());
				List<Object> object = docSource.getMainDocumentPart().getContent();
				for (Object o : object)docDest.getMainDocumentPart().getContent().add(o);
			}

		}
		return docDest;
	}
	
	
	
	
	 
	 * Questo metodo trasforma il documento Word docx in PDF
	 * 
	 
	
	private NodeRef saveWordToPDF(NodeRef parentFolder,String fileName,WordprocessingMLPackage wordMLPackage)
	{

	NodeRef ref = fileFolderService.create(parentFolder, fileName+".pdf", ContentModel.TYPE_CONTENT).getNodeRef();
	ContentWriter cw = contentService.getWriter(ref, ContentModel.PROP_CONTENT, true);
	cw.setMimetype(MimetypeMap.MIMETYPE_WORD);
	try {

	wordMLPackage.save(cw.getContentOutputStream());
	return convertWordToPDF (ref);

	} catch (ContentIOException e) {

	e.printStackTrace();

	} catch (Docx4JException e) {

	e.printStackTrace();

	} 
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
		catch (FileExistsException e1)e1.printStackTrace();
		catch(Exception e) e.printStackTrace();
		return null;

	}
	*/
}