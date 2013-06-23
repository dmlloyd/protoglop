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

package org.wildfly.core.management.processor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import nu.xom.Attribute;
import nu.xom.Comment;
import nu.xom.Document;
import nu.xom.Element;

import javax.tools.Diagnostic;

import static javax.tools.Diagnostic.Kind.ERROR;
import static org.wildfly.core.management.processor.SchemaInfo.XS;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class SchemaGeneratorContext {

    private final GeneratorContext context;
    private final SchemaInfo info;
    private final Set<ResourceInfo> generatedResources = new HashSet<ResourceInfo>();
    private final Map<String, Element> rootElements = new TreeMap<String, Element>();
    private final Map<String, Object> rootElementOwners = new HashMap<String, Object>();
    private final Map<String, Element> rootTypes = new TreeMap<String, Element>();
    private final Map<String, Object> rootTypeOwners = new HashMap<String, Object>();

    public SchemaGeneratorContext(final GeneratorContext generatorContext, final SchemaInfo schemaInfo) {
        context = generatorContext;
        info = schemaInfo;
    }

    public GeneratorContext getContext() {
        return context;
    }

    public boolean addGeneratedResource(ResourceInfo resourceInfo) {
        return generatedResources.add(resourceInfo);
    }

    public SchemaInfo getInfo() {
        return info;
    }

    public void addRootElement(String name, Element element) {
        if (rootElements.containsKey(name)) {
            context.getEnv().getMessager().printMessage(Diagnostic.Kind.ERROR, "More than one root element declared for " + name + " in schema " + info.getXmlNamespace());
        } else {
            rootElements.put(name, element);
        }
    }

    public void generate() {
        final Element schemaElement = new Element("xs:schema", SchemaInfo.XS);
        final Document document = new Document(schemaElement);
        final String xmlNamespace = info.getXmlNamespace();

        // create XML skeleton

        schemaElement.addNamespaceDeclaration("", xmlNamespace);
        schemaElement.addNamespaceDeclaration("xs", SchemaInfo.XS);

        schemaElement.addAttribute(new Attribute("targetNamespace", xmlNamespace));
        schemaElement.addAttribute(new Attribute("elementFormDefault", "qualified"));
        schemaElement.addAttribute(new Attribute("attributeFormDefault", "unqualified"));

        // add all resource info to the schema

        for (RootResourceInfo resourceInfo : info.getResources()) {
            resourceInfo.addToSchema(this);
            resourceInfo.generateClasses(context);
        }

        // build the schema into a document

        schemaElement.appendChild(new Comment("\nRoot elements\n"));
        for (Element element : rootElements.values()) {
            schemaElement.appendChild(element);
        }
        schemaElement.appendChild(new Comment("\nElement types\n"));
        for (Element element : rootTypes.values()) {
            schemaElement.appendChild(element);
        }

        // add it to the global context

        if (! context.addDocument(info, document)) {
            context.getEnv().getMessager().printMessage(ERROR, "Schema info " + this + " generated more than once");
        }
    }

    public Element addRootElement(final String xmlName, final Object owner, final javax.lang.model.element.Element programElement) {
        final Element element = new Element("xs:element", SchemaInfo.XS);
        element.addAttribute(new Attribute("name", xmlName));
        if (rootElements.containsKey(xmlName)) {
            if (rootElementOwners.get(xmlName) != owner) {
                getContext().getEnv().getMessager().printMessage(ERROR, "Duplicate root element '" + xmlName + "' generated in schema '" + info.getXmlNamespace() + "'", programElement);
            } else {
                return null;
            }
        } else {
            // if error was reported, we don't want to actually add these, but we still want to proceed as if we had
            rootElementOwners.put(xmlName, owner);
            rootElements.put(xmlName, element);
        }
        return element;
    }

    public Element addRootComplexType(final String xmlTypeName, final Object owner, final javax.lang.model.element.Element programElement) {
        final Element element = new Element("xs:complexType", SchemaInfo.XS);
        element.addAttribute(new Attribute("name", xmlTypeName));
        if (rootTypes.containsKey(xmlTypeName)) {
            if (rootTypeOwners.get(xmlTypeName) != owner) {
                getContext().getEnv().getMessager().printMessage(ERROR, "Duplicate root type '" + xmlTypeName + "' generated in schema '" + info.getXmlNamespace() + "'", programElement);
            } else {
                return null;
            }
        } else {
            // if error was reported, we don't want to actually add these, but we still want to proceed as if we had
            rootTypeOwners.put(xmlTypeName, owner);
            rootTypes.put(xmlTypeName, element);
        }
        return element;
    }
}
