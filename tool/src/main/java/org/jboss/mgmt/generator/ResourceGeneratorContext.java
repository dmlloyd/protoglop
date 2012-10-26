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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import nu.xom.Element;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JMethod;

import javax.tools.Diagnostic;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ResourceGeneratorContext {

    private final SchemaGeneratorContext context;
    private final RootResourceInfo resourceInfo;
    private final ResourceGeneratorContext parent;
    private final Map<String, Element> nestedElements = new LinkedHashMap<String, Element>();
    private final Map<String, Element> attributeDefinitions = new TreeMap<String, Element>();
    private final JClass resourceInterface;
    private final JDefinedClass resourceImplClass;
    private final JMethod resourceImplConstructor;
    private final JBlock resourceImplConstructorInitBlock;

    public ResourceGeneratorContext(final SchemaGeneratorContext context, final RootResourceInfo resourceInfo, final ResourceGeneratorContext parent) {
        this.context = context;
        this.resourceInfo = resourceInfo;
        this.parent = parent;
    }

    public ResourceGeneratorContext(final SchemaGeneratorContext context, final RootResourceInfo resourceInfo) {
        this(context, resourceInfo, null);
    }

    public SchemaGeneratorContext getContext() {
        return context;
    }

    public RootResourceInfo getResourceInfo() {
        return resourceInfo;
    }

    public ResourceGeneratorContext getParent() {
        return parent;
    }

    public Collection<Element> getNestedElements() {
        return nestedElements.values();
    }

    public Collection<Element> getXmlAttributeDefinitions() {
        return attributeDefinitions.values();
    }

    public void addXmlAttribute(final String name, final Element definition) {
        if (attributeDefinitions.containsKey(name)) {
            context.getContext().getEnv().getMessager().printMessage(Diagnostic.Kind.ERROR, "Duplicate attribute " + name + " defined in " + resourceInfo);
        } else {
            attributeDefinitions.put(name, definition);
        }
    }

    public JClass getResourceInterface() {
        return resourceInterface;
    }

    public JDefinedClass getResourceImplClass() {
        return resourceImplClass;
    }

    public JMethod getResourceImplConstructor() {
        return resourceImplConstructor;
    }

    public JBlock getResourceImplConstructorInitBlock() {
        return resourceImplConstructorInitBlock;
    }
}
