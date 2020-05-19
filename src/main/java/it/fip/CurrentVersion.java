package it.fip;

import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBElement;

import org.alfresco.repo.action.ActionImpl;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionStatus;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.version.VersionHistory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.ContentAccessor;
import org.docx4j.wml.Text;

/*
 * Code below is an implementation of different parts, adapted to our specific case. 
 * Starting from 6phere project on Github, that you can find here:
 * https://github.com/6phere/alf-docx4j
 * 
 * Instead of the standard docx4j Variable Replace there's a method from stackoverflow:
 * https://stackoverflow.com/questions/20484722/docx4j-how-to-replace-placeholder-with-value
 * Thanks demotics2002
 * 
 * Other things from Alfresco SDK documentation and docx4j source code.
 */

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

	public void variableReplace(NodeRef nodeRef) throws ContentIOException, Docx4JException {
		try {
            ContentReader reader = this.serviceRegistry.getContentService().getReader(nodeRef, ContentModel.PROP_CONTENT);
            WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.load(reader.getContentInputStream());
            //MainDocumentPart documentPart = wordMLPackage.getMainDocumentPart();
            String versioneAttuale = this.getUltimaVersione(nodeRef);
            List<Object> texts = getAllElementFromObject(wordMLPackage.getMainDocumentPart(), Text.class);
            searchAndReplace(texts, new HashMap<String, String>() {
                private static final long serialVersionUID = -3834567736069978880L;
                {
                    this.put("${versioneAttuale}", versioneAttuale);
                }
                @Override
                public String get(Object key) {
                    // TODO Auto-generated method stub
                    return super.get(key);
                }
            });
            /*
             * List<Object> texts = getAllElementFromObject(documentPart, Text.class);
            HashMap<String, String> mapping = new HashMap<String, String>();
            mapping.put("versioneAttuale", versioneAttuale);
            logger.debug("Ultima versione: " + versioneAttuale);
            mapping.put("dataModifica", dataModifica);
            wordMLPackage.getMainDocumentPart().variableReplace(mapping);
            */
            ContentWriter writer = this.serviceRegistry.getContentService().getWriter(nodeRef, ContentModel.PROP_CONTENT, true);
            wordMLPackage.save(writer.getContentOutputStream());
        }
		catch (ContentIOException | Docx4JException | JAXBException e1) {
				e1.printStackTrace();} 
		
    }
	
	private static List<Object> getAllElementFromObject(Object obj,Class<?> toSearch)
				throws JAXBException {
        List<Object> result = new ArrayList<>();
        if (obj instanceof JAXBElement)
            obj = ((JAXBElement<?>) obj).getValue();

        if (obj.getClass().equals(toSearch))
            result.add(obj);
        else if (obj instanceof ContentAccessor) {
            List<?> children = ((ContentAccessor) obj).getContent();
            for (Object child : children) {
                result.addAll(getAllElementFromObject(child, toSearch));
            }

        }
        return result;
    }

	
	public String getDocumentName(NodeRef nodeRef) {
		NodeService nodeService = serviceRegistry.getNodeService();
        return nodeService.getProperty(nodeRef, ContentModel.PROP_NAME).toString();
	}

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
        List<int[]> toNullify = new ArrayList<>();
        int[] currentNullifyProps = new int[4];

        // Do scan of els and immediately insert value
        for(int i = 0; i<texts.size(); i++){
            Object text = texts.get(i);
            Text textElement = (Text) text;
            StringBuilder newVal = new StringBuilder();
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
                        newVal.append(textSofar.toString()).append(null == values.get(sb.toString()) ? sb.toString() : values.get(sb.toString()));
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
            newVal.append(textSofar.toString());
            textElement.setValue(newVal.toString());
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
	/*
	 private void replacePlaceholder(WordprocessingMLPackage template,
            String name, String placeholder) {
        List<Object> texts = getAllElementFromObject(
                template.getMainDocumentPart(), Text.class);

        for (Object text : texts) {
            Text textElement = (Text) text;
            if (textElement.getValue().equals(placeholder)) {
                textElement.setValue(name);
            }
        }
    }
	  
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