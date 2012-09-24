/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.mgmt.generator;

import org.jboss.mgmt.NestedBuilder;
import org.jboss.mgmt.annotation.RootResource;
import org.jboss.mgmt.annotation.RuntimeMode;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface RootResourceBuilder extends GeneralResourceBuilder, NestedBuilder<Session> {

    AttributeBuilder<? extends RootResourceBuilder> attribute(final String name);

    RootResourceBuilder description(String description);

    RootResourceBuilder operationHook(String opName, String version, ExecutableElement method);

    RootResourceBuilder listener(DeclaredType listener, RuntimeMode... modes);

    RootResourceBuilder provides(String token);

    RootResourceBuilder schemaLocation(String location);

    RootResourceBuilder version(String version);

    RootResourceBuilder kind(RootResource.Kind kind);

    RootResourceBuilder namespace(String namespace);

    SubResourceBuilder<? extends RootResourceBuilder> subResource(String name, boolean named);

    RootResourceBuilder xmlName(String name);

    Session end();
}
