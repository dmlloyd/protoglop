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

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import nu.xom.Element;

import javax.tools.Diagnostic;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class SchemaGeneratorContext {

    private final GeneratorContext context;
    private final NewSchemaInfo info;
    private final Map<String, Element> rootElements = new TreeMap<String, Element>();
    private final Map<String, Element> rootTypes = new TreeMap<String, Element>();

    public SchemaGeneratorContext(final GeneratorContext generatorContext, final NewSchemaInfo schemaInfo) {
        context = generatorContext;
        info = schemaInfo;
    }

    public GeneratorContext getContext() {
        return context;
    }

    public NewSchemaInfo getInfo() {
        return info;
    }

    public void addRootElement(String name, Element element) {
        if (rootElements.containsKey(name)) {
            context.getEnv().getMessager().printMessage(Diagnostic.Kind.ERROR, "More than one root element declared for " + name + " in schema " + info.getXmlNamespace());
        } else {
            rootElements.put(name, element);
        }
    }

    public void addRootType(String name, Element type) {
        if (rootTypes.containsKey(name)) {
            context.getEnv().getMessager().printMessage(Diagnostic.Kind.ERROR, "More than one root type declared for " + name + " in schema " + info.getXmlNamespace());
        } else {
            rootTypes.put(name, type);
        }
    }

    public Collection<Element> getRootElements() {
        return rootElements.values();
    }

    public Collection<Element> getRootTypes() {
        return rootTypes.values();
    }
}
