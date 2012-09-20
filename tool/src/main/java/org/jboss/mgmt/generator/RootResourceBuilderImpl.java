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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.jboss.mgmt.annotation.RuntimeMode;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class RootResourceBuilderImpl extends GeneralResourceBuilderImpl<RootResourceBuilderImpl> implements RootResourceBuilder {
    private final Session session;
    private final Map<String, String> namespaces = new HashMap<String, String>();
    private final Map<String, String> schemaLocations = new HashMap<String, String>();

    RootResourceBuilderImpl(final Session session, final String type, final DeclaredType resourceInterface) {
        super(type, resourceInterface);
        this.session = session;
    }

    public RootResourceBuilder description(final Locale locale, final String description) {
        super.description(locale, description);
        return this;
    }

    public RootResourceBuilder operationHook(final String opName, final String version, final ExecutableElement method) {
        super.operationHook(opName, version, method);
        return this;
    }

    public RootResourceBuilder listener(final TypeMirror listener, final RuntimeMode... modes) {
        super.listener(listener, modes);
        return this;
    }

    public RootResourceBuilder provides(final String token) {
        super.provides(token);
        return this;
    }

    @SuppressWarnings("unchecked")
    public SubResourceBuilder<RootResourceBuilder> subResource(final String address, final boolean named) {
        return (SubResourceBuilder<RootResourceBuilder>) super.subResource(address, named);
    }

    public RootResourceBuilder xmlName(final String xmlName) {
        super.xmlName(xmlName);
        return this;
    }

    public RootResourceBuilder addXmlNamespace(final String namespace, final String version, final String schemaLocation) {
        schemaLocations.put(namespace, schemaLocation);
        return addXmlNamespace(namespace, version);
    }

    public RootResourceBuilder addXmlNamespace(final String namespace, final String version) {
        namespaces.put(namespace, version);
        ((SessionImpl)session).addNamespace(namespace, this);
        return this;
    }

    public Session end() {
        return session;
    }

    Map<String, String> getNamespaces() {
        return namespaces;
    }

    Map<String, String> getSchemaLocations() {
        return schemaLocations;
    }
}
