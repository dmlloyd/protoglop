package org.jboss.as.threads;

import org.jboss.mgmt.Resource;
import org.jboss.mgmt.annotation.Attribute;
import org.jboss.mgmt.annotation.Required;
import org.jboss.mgmt.annotation.XmlName;
import org.jboss.mgmt.annotation.XmlRender;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
@XmlName("thread-factory")
public interface ThreadFactoryResource extends Resource {
    @XmlRender(as = XmlRender.As.ATTRIBUTE)
    @Attribute()
    @Required(false)
    String getGroupName();

    @Attribute
    @Required(false)
    @XmlRender(as = XmlRender.As.ATTRIBUTE)
    String getThreadNamePattern();

    @Required(false)
    @XmlRender(as = XmlRender.As.ATTRIBUTE)
    @Attribute(/*validators = {IntRangeValidator.class}*/)
    int getPriority();

}
