package documentversion;

import org.alfresco.repo.policy.Behaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.version.VersionServicePolicies;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.AspectMissingException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.version.ReservedVersionNameException;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

public class CurrentVersion implements VersionService {

    private static Log logger = LogFactory.getLog(CurrentVersion.class);
    private ServiceRegistry serviceRegistry;
    private PolicyComponent policyComponent;
    private Behaviour afterCreateVersion;

    public void init() {
        
    }

    @Override
    public StoreRef getVersionStoreReference() {
        return null;
    }

    @Override
    public boolean isAVersion(NodeRef nodeRef) {
        return false;
    }

    @Override
    public boolean isVersioned(NodeRef nodeRef) {
        return false;
    }

    @Override
    public Version createVersion(NodeRef nodeRef, Map<String, Serializable> map) throws ReservedVersionNameException, AspectMissingException {
        return null;
    }

    @Override
    public Collection<Version> createVersion(NodeRef nodeRef, Map<String, Serializable> map, boolean b) throws ReservedVersionNameException, AspectMissingException {
        return null;
    }

    @Override
    public Collection<Version> createVersion(Collection<NodeRef> collection, Map<String, Serializable> map) throws ReservedVersionNameException, AspectMissingException {
        return null;
    }

    @Override
    public VersionHistory getVersionHistory(NodeRef nodeRef) throws AspectMissingException {
        return null;
    }

    @Override
    public Version getCurrentVersion(NodeRef nodeRef) {
        return this.getCurrentVersion(nodeRef);
    }

    @Override
    public void revert(NodeRef nodeRef) {

    }

    @Override
    public void revert(NodeRef nodeRef, boolean b) {

    }

    @Override
    public void revert(NodeRef nodeRef, Version version) {

    }

    @Override
    public void revert(NodeRef nodeRef, Version version, boolean b) {

    }

    @Override
    public NodeRef restore(NodeRef nodeRef, NodeRef nodeRef1, QName qName, QName qName1) {
        return null;
    }

    @Override
    public NodeRef restore(NodeRef nodeRef, NodeRef nodeRef1, QName qName, QName qName1, boolean b) {
        return null;
    }

    @Override
    public void deleteVersionHistory(NodeRef nodeRef) throws AspectMissingException {

    }

    @Override
    public void deleteVersion(NodeRef nodeRef, Version version) {

    }

    @Override
    public void ensureVersioningEnabled(NodeRef nodeRef, Map<QName, Serializable> map) {

    }

    @Override
    public void registerVersionLabelPolicy(QName qName, VersionServicePolicies.CalculateVersionLabelPolicy calculateVersionLabelPolicy) {

    }

}