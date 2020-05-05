package documentversion;

import org.alfresco.repo.policy.Behaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class CurrentVersion implements VersionService {

    private static Log logger = LogFactory.getLog(CurrentVersion.class);
    private ServiceRegistry serviceRegistry;
    private PolicyComponent policyComponent;
    private Behaviour afterCreateVersion;

    @Override
    public Version getCurrentVersion(NodeRef nodeRef) {
        Version versione = null;
        if (nodeRef != null) {
            versione = this.getCurrentVersion(nodeRef);
        }
        return versione;
    }

}