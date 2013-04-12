package org.jboss.as.xts;


import org.jboss.mgmt.Resource;
import org.jboss.mgmt.annotation.Attribute;
import org.jboss.mgmt.annotation.Provides;
import org.jboss.mgmt.annotation.RootResource;
import org.jboss.mgmt.annotation.XmlName;
import org.jboss.mgmt.annotation.XmlRender;

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
public interface XTSSubsystemResource extends Resource {

    @Attribute
    @XmlRender(as = XmlRender.As.ATTRIBUTE)
    String getEnvironmentUrl();
}
