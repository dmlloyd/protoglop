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

import java.util.Locale;
import org.jboss.mgmt.annotation.RuntimeMode;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface RootResourceBuilder extends GeneralResourceBuilder, SubBuilder<Session> {

    AttributeBuilder<? extends RootResourceBuilder> attribute();

    RootResourceBuilder description(Locale locale, String description);

    RootResourceBuilder operationHook(String opName, String version, ExecutableElement method);

    RootResourceBuilder listener(TypeMirror listener, RuntimeMode... modes);

    RootResourceBuilder provides(String token);

    RootResourceBuilder addXmlNamespace(String namespace, String version, String schemaLocation);

    RootResourceBuilder addXmlNamespace(String namespace, String version);

    SubResourceBuilder<RootResourceBuilder> subResource(String address, boolean named);

    RootResourceBuilder xmlName(String name);
}
