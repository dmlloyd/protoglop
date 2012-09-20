/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package example1;

import java.util.List;
import org.jboss.mgmt.annotation.ModelRoot;
import org.jboss.mgmt.annotation.Provides;
import org.jboss.mgmt.annotation.SubResource;
import org.jboss.mgmt.annotation.Versions;
import org.jboss.mgmt.annotation.xml.Mapping;
import org.jboss.mgmt.annotation.xml.XmlName;
import org.jboss.mgmt.annotation.xml.XmlVersionMapping;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@Provides("logging")
@ModelRoot(type = "subsystem")
@XmlName("subsystem")
@Versions({"1.0", "1.1"})
@XmlVersionMapping({
    @Mapping(xmlns = "ext:core.logging:1.0", version = "1.0", schemaLocation = "http://docs.jboss.org/foo/blah/1.0"),
    @Mapping(xmlns = "ext:core.logging:1.1", version = "1.1", schemaLocation = "http://docs.jboss.org/foo/blah/1.1")
})
public interface LoggingSubsystemResource {

    @SubResource
    List<String> getHandlerNames();

    HandlerResource getHandler(String name);

    @SubResource
    List<String> getLoggerNames();

    LoggerResource getLogger(String name);
}
