package org.wildfly.as.xts;


import org.wildfly.core.management.Node;
import org.wildfly.core.management.annotation.Attribute;
import org.wildfly.core.management.annotation.Provides;
import org.wildfly.core.management.annotation.RootResource;
import org.wildfly.core.management.annotation.XmlName;
import org.wildfly.core.management.annotation.XmlRender;
import org.wildfly.core.management.annotation.XmlName;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
@XTS_1_0
@Provides("xts")
@XmlName("subsystem")
@RootResource(
        type = "subsystem",
        name = "xts"
)
public interface XTSSubsystemResource extends Node {

    @Attribute
    @XmlRender(as = XmlRender.As.ATTRIBUTE)
    String getEnvironmentUrl();
}
