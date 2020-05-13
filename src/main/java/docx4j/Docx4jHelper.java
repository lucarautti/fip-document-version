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
import org.alfresco.service.cmr.model.*;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
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
	private FileFolderService fileFolderService;
	private ContentService contentService;
	private static Log logger = LogFactory.getLog(Docx4jHelper.class);
	
	org.docx4j.wml.ObjectFactory foo = Context.getWmlObjectFactory();
	private static final long serialVersionUID = 1L;


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
	
	
	public void variableReplace(NodeRef nodeRef) throws ContentIOException, Docx4JException {
		
		ContentReader reader = this.serviceRegistry.getContentService().getReader(nodeRef, ContentModel.PROP_CONTENT);
		WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.load(reader.getContentInputStream());
		List<Object> texts = getAllElementFromObject(wordMLPackage.getMainDocumentPart(), Text.class);
		String versioneAttuale = this.getUltimaVersione(nodeRef);
		String dataModifica = this.getDataCorrente();
		Docx4jHelper.searchAndReplace(texts, new HashMap<String, String>() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 4561563843875288596L;

			{
				this.put("${versioneAttuale}", versioneAttuale);
				this.put("${dataModifica}", dataModifica);
			}

			@Override
            public String get(Object key) {
                // TODO Auto-generated method stub
                return super.get(key);
            }
		});   
		
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

	
	public String getDocumentName(NodeRef nodeRef) {
		NodeService nodeService = serviceRegistry.getNodeService();
		String nodeName = nodeService.getProperty(nodeRef, ContentModel.PROP_NAME).toString();
		return nodeName;
	}

	public NodeRef getTemplateInstance(NodeRef templateRef, NodeRef targetRef, String name) {
		NodeRef instance = this.serviceRegistry.getCopyService().copy(templateRef, targetRef,
				org.alfresco.model.ContentModel.ASSOC_CONTAINS, org.alfresco.model.ContentModel.ASSOC_ORIGINAL);
		this.serviceRegistry.getNodeService().setProperty(instance, org.alfresco.model.ContentModel.PROP_NAME, name);
		return instance;
	}

	
	/*
	 * * Questo metodo trasforma il documento Word docx in PDF
	 */
	  
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
	
	/*
	private void replacePlaceholder(WordprocessingMLPackage template,String name, String placeholder) {
		
		List<Object> texts = getAllElementFromObject(template.getMainDocumentPart(), Text.class);
		for (Object text : texts) {
			Text textElement = (Text) text;
			if (textElement.getValue().equals(placeholder)) {
				textElement.setValue(name);
            }
        }
    }
	*/
	
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
*/
	
	public static void searchAndReplace(List<Object> texts, Map<String, String> values){

        // -- scan all expressions  
        // Will later contain all the expressions used though not used at the moment
        List<String> els = new ArrayList<String>(); 

        StringBuilder sb = new StringBuilder();
        int PASS = 0;
        int PREPARE = 1;
        int READ = 2;
        int mode = PASS;

        // to nullify
        List<int[]> toNullify = new ArrayList<int[]>();
        int[] currentNullifyProps = new int[4];

        // Do scan of els and immediately insert value
        for(int i = 0; i<texts.size(); i++){
            Object text = texts.get(i);
            Text textElement = (Text) text;
            String newVal = "";
            String v = textElement.getValue();
//          System.out.println("text: "+v);
            StringBuilder textSofar = new StringBuilder();
            int extra = 0;
            char[] vchars = v.toCharArray();
            for(int col = 0; col<vchars.length; col++){
                char c = vchars[col];
                textSofar.append(c);
                switch(c){
                case '$': {
                    mode=PREPARE;
                    sb.append(c);
//                  extra = 0;
                } break;
                case '{': {
                    if(mode==PREPARE){
                        sb.append(c);
                        mode=READ;
                        currentNullifyProps[0]=i;
                        currentNullifyProps[1]=col+extra-1;
                        System.out.println("extra-- "+extra);
                    } else {
                        if(mode==READ){
                            // consecutive opening curl found. just read it
                            // but supposedly throw error
                            sb = new StringBuilder();
                            mode=PASS;
                        }
                    }
                } break;
                case '}': {
                    if(mode==READ){
                        mode=PASS;
                        sb.append(c);
                        els.add(sb.toString());
                        newVal +=textSofar.toString()
                                +(null==values.get(sb.toString())?sb.toString():values.get(sb.toString()));
                        textSofar = new StringBuilder();
                        currentNullifyProps[2]=i;
                        currentNullifyProps[3]=col+extra;
                        toNullify.add(currentNullifyProps);
                        currentNullifyProps = new int[4];
                        extra += sb.toString().length();
                        sb = new StringBuilder();
                    } else if(mode==PREPARE){
                        mode = PASS;
                        sb = new StringBuilder();
                    }
                }
                default: {
                    if(mode==READ) sb.append(c);
                    else if(mode==PREPARE){
                        mode=PASS;
                        sb = new StringBuilder();
                    }
                }
                }
            }
            newVal +=textSofar.toString();
            textElement.setValue(newVal);
        }

        // remove original expressions
        if(toNullify.size()>0)
        for(int i = 0; i<texts.size(); i++){
            if(toNullify.size()==0) break;
            currentNullifyProps = toNullify.get(0);
            Object text = texts.get(i);
            Text textElement = (Text) text;
            String v = textElement.getValue();
            StringBuilder nvalSB = new StringBuilder();
            char[] textChars = v.toCharArray();
            for(int j = 0; j<textChars.length; j++){
                char c = textChars[j];
                if(null==currentNullifyProps) {
                    nvalSB.append(c);
                    continue;
                }
                // I know 100000 is too much!!! And so what???
                int floor = currentNullifyProps[0]*100000+currentNullifyProps[1];
                int ceil = currentNullifyProps[2]*100000+currentNullifyProps[3];
                int head = i*100000+j;
                if(!(head>=floor && head<=ceil)){
                    nvalSB.append(c);
                } 

                if(j>currentNullifyProps[3] && i>=currentNullifyProps[2]){
                    toNullify.remove(0);
                    if(toNullify.size()==0) {
                        currentNullifyProps = null;
                        continue;
                    }
                    currentNullifyProps = toNullify.get(0);
                }
            }
            textElement.setValue(nvalSB.toString());
        }
    }
}