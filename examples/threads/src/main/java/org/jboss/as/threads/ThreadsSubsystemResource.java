package org.jboss.as.threads;

import java.util.Map;
import org.jboss.mgmt.Resource;
import org.jboss.mgmt.annotation.Provides;
import org.jboss.mgmt.annotation.RootResource;
import org.jboss.mgmt.annotation.SubResource;
import org.jboss.mgmt.annotation.XmlName;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
@Threads_1_0
@Provides("threads")
@XmlName("subsystem")
@RootResource(
        type = "subsystem",
        name = "threads"
)
public interface ThreadsSubsystemResource extends Resource {

    @SubResource
    Map<String, ThreadFactoryResource> getThreadFactories();
}
