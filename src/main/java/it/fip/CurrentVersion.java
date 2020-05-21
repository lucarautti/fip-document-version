package it.fip;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ActionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionStatus;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.InvalidTypeException;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.namespace.InvalidQNameException;
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
            ((ActionImpl) action).setExecutionEndDate(new Date());
            ((ActionImpl) action).setExecutionStatus(ActionStatus.Failed);
            ((ActionImpl) action).setExecutionFailureMessage("Si è verificato un problema. " + e.getMessage());
            throw new RuntimeException(e);
        }
    }


    /*
     * This is the main method, recalled by the custom action.
     * With the nodeRef passed in, it executes other custom methods following ths logic:
     * open original docx --> find the placeholder --> replace it --> save a temp docx --> convert to pdf
     */
    private void mainLogic(NodeRef nodeRef) throws ContentIOException {
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
            String tempDocxName = getDocumentName(nodeRef) + "_temp.docx";
            String pdfName = getDocumentName(nodeRef) + ".pdf";
            NodeRef tempDocxNode = createContentNode(parent, tempDocxName, originalDocx);
            transformPdfNode(parent, tempDocxNode, pdfName);
            deleteNode(tempDocxNode);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    /*
     * Using the Alfresco VersionService API this method get the latest version
     * given a nodeRef.
     * More info here: https://dev.alfresco.com/resource/docs/java/org/alfresco/service/cmr/version/VersionHistory.html
     * and here: https://docs.alfresco.com/5.2/references/dev-services-version.html
     */
    private String getUltimaVersione(NodeRef versionableNode) {

        String versione = "";
        VersionHistory versionHistory = serviceRegistry.getVersionService().getVersionHistory(versionableNode);
        if (versionHistory != null) {
            logger.debug("Numero di versioni: " + versionHistory.getAllVersions().size());
            logger.debug("Ultima versione: " + versionHistory.getHeadVersion().getVersionLabel());
            versione = versionHistory.getHeadVersion().getVersionLabel();
        } else {
            logger.debug("Nodo non versionabile");
        }
        return versione;
    }

    /*
     * Get the document name given a specific nodeRef, using Alfresco NodeService
     * and ContentModel property. Splitting before the dot remove the extension.
     */
    private String getDocumentName(NodeRef nodeRef) {
        NodeService nodeService = serviceRegistry.getNodeService();
        String nomeIntero = nodeService.getProperty(nodeRef, ContentModel.PROP_NAME).toString();
        return nomeIntero.split("\\.")[0];
    }

    /*
     * Create a new node with docx mimetype and save it passing the WordprocessingMLPackage
     * of the original docx with variable replaced.
     * This way has been adopted to preserve the original docx, creating a temp one,
     * so you can start a new workflow with the old docx.
     * Return the nodeRef of the temp docx created.
     */
    private NodeRef createContentNode(NodeRef parent, String name, WordprocessingMLPackage oldDocx) {

        NodeRef node = null;
        try {
            Map<QName, Serializable> props = new HashMap<>(1);
            props.put(ContentModel.PROP_NAME, name);
            node = this.serviceRegistry.getNodeService().createNode(
                    parent,
                    ContentModel.ASSOC_CONTAINS,
                    QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, name),
                    ContentModel.TYPE_CONTENT,
                    props).getChildRef();
            ContentWriter writer = this.serviceRegistry.getContentService().getWriter(node, ContentModel.PROP_CONTENT, true);
            writer.setMimetype(MimetypeMap.MIMETYPE_OPENXML_WORDPROCESSING);
            oldDocx.save(writer.getContentOutputStream());

        } catch (InvalidTypeException | InvalidQNameException | ContentIOException | InvalidNodeRefException | Docx4JException e) {
            e.printStackTrace();
        }
        return node;
    }

    /*
     * Same as the previous method.
     * It performs a check at the beginning, so if a node with the same name exist it
     * will be deleted.
     * Then it transform the temp docx to pdf.
     */
    private void transformPdfNode(NodeRef parent, NodeRef tempNode, String name) {

        NodeRef checkNode = this.serviceRegistry.getNodeService().getChildByName(
        					parent,
							ContentModel.ASSOC_CONTAINS,
							name);
        NodeRef pdfNode;
        try {
            if (checkNode != null) {
                this.deleteNode(checkNode);
            }
            Map<QName, Serializable> props = new HashMap<>(1);
            props.put(ContentModel.PROP_NAME, name);
            pdfNode = this.serviceRegistry.getNodeService().createNode(
            		parent,
					ContentModel.ASSOC_CONTAINS,
                    QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, name),
                    ContentModel.TYPE_CONTENT,
                    props).getChildRef();
            ContentReader tempDocx = this.serviceRegistry.getContentService().getReader(tempNode, ContentModel.PROP_CONTENT);
            ContentWriter pdfWriter = this.serviceRegistry.getContentService().getWriter(pdfNode, ContentModel.PROP_CONTENT, true);
            pdfWriter.setMimetype(MimetypeMap.MIMETYPE_PDF);
            this.serviceRegistry.getContentService().transform(tempDocx, pdfWriter);
        } catch (InvalidTypeException | InvalidNodeRefException | ContentIOException | NoTransformerException | InvalidQNameException e) {
            e.printStackTrace();
        }
    }

    private void deleteNode(NodeRef nodeToDelete) {
        this.serviceRegistry.getNodeService().addAspect(nodeToDelete, ContentModel.ASPECT_TEMPORARY, null);
        this.serviceRegistry.getNodeService().deleteNode(nodeToDelete);
    }
}