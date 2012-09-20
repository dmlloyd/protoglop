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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.jboss.mgmt.annotation.RuntimeMode;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
abstract class GeneralResourceBuilderImpl<THIS extends GeneralResourceBuilderImpl<THIS>> implements GeneralResourceBuilder {
    private final String type;
    private final DeclaredType resourceInterface;
    private final Set<String> provides = new HashSet<String>();
    private final List<AttributeBuilderImpl<?>> attributes = new ArrayList<AttributeBuilderImpl<?>>();

    private String xmlName;
    private String name;

    protected GeneralResourceBuilderImpl(final String type, final DeclaredType resourceInterface) {
        this.type = type;
        this.resourceInterface = resourceInterface;
    }

    @SuppressWarnings("unchecked")
    final THIS _this() { return (THIS) this; }

    public AttributeBuilder<THIS> attribute() {
        return new AttributeBuilderImpl<THIS>(_this());
    }

    public GeneralResourceBuilder description(final Locale locale, final String description) {
        return this;
    }

    public GeneralResourceBuilder operationHook(final String opName, final String version, final ExecutableElement method) {
        return this;
    }

    public GeneralResourceBuilder listener(final TypeMirror listener, final RuntimeMode... modes) {
        return this;
    }

    public GeneralResourceBuilder provides(final String token) {
        provides.add(token);
        return this;
    }

    public GeneralResourceBuilder subResource(final String address, final boolean named) {
        return this;
    }

    public GeneralResourceBuilder xmlName(final String xmlName) {
        this.xmlName = xmlName;
        return this;
    }

    String getXmlName() {
        return xmlName;
    }

    String getType() {
        return type;
    }

    DeclaredType getResourceInterface() {
        return resourceInterface;
    }

    Set<String> getProvides() {
        return provides;
    }

    List<AttributeBuilderImpl<?>> getAttributes() {
        return attributes;
    }

    String getName() {
        return name;
    }
}
