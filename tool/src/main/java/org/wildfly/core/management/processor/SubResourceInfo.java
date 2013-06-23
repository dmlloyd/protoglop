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

import nu.xom.Attribute;
import nu.xom.Element;
import org.jboss.jdeparser.JDefinedClass;
import org.jboss.jdeparser.JMethod;

import static org.wildfly.core.management.processor.SchemaInfo.XS;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class SubResourceInfo implements ResourceMember {

    private final ResourceTypeInfo resourceTypeInfo;
    private final String type;
    private final String name;
    private final String xmlName;
    private final String xmlTypeName;
    private final boolean requiresUnique;
    private final ResourceInfo[] knownChildren;

    /**
     * Construct a new instance.
     *
     * @param resourceTypeInfo the info for the resource type ({@code null} if none given)
     * @param type the symbolic type name of candidate members or {@code null}
     * @param name the name in the model in XML form (not {@code null})
     * @param xmlName the XML wrapper element name
     * @param xmlTypeName the XML wrapper element type name
     * @param requiresUnique
     * @param knownChildren same-schema candidate child resources
     */
    SubResourceInfo(final ResourceTypeInfo resourceTypeInfo, final String type, final String name, final String xmlName, final String xmlTypeName, final boolean requiresUnique, final ResourceInfo[] knownChildren) {
        this.type = type;
        this.name = name;
        this.xmlName = xmlName;
        this.xmlTypeName = xmlTypeName;
        this.requiresUnique = requiresUnique;
        this.knownChildren = knownChildren;
        this.resourceTypeInfo = resourceTypeInfo;
    }

    public ResourceTypeInfo getResourceTypeInfo() {
        return resourceTypeInfo;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public boolean isRequiresUnique() {
        return requiresUnique;
    }

    public ResourceInfo[] getKnownChildren() {
        return knownChildren;
    }

    public void addToSchema(final SchemaGeneratorContext ctxt, final Element typeElement, final Element seqElement) {
        // wrapper element
        final Element wrapperElement = new Element("xs:element", XS);
        wrapperElement.addAttribute(new Attribute("name", xmlName));
        wrapperElement.addAttribute(new Attribute("type", xmlTypeName));
        seqElement.appendChild(wrapperElement);

        final Element rootType = ctxt.addRootComplexType(xmlTypeName, this, null);
        if (rootType == null) {
            // done
            return;
        }

        final Element choiceElement = new Element("xs:choice", XS);
        for (ResourceInfo knownChild : knownChildren) {
            // make sure this XML type is in the schema
            knownChild.addToSchema(ctxt);
            // add this element to our content
            final Element elementElement = new Element("xs:element", XS);
            elementElement.addAttribute(new Attribute("name", knownChild.getXmlName()));
            elementElement.addAttribute(new Attribute("type", knownChild.getXmlTypeName()));
            choiceElement.appendChild(elementElement);
        }
        // add an xs:any element if one is needed
        if (resourceTypeInfo != null) {
            final Element anyElement = new Element("xs:any", XS);
            anyElement.addAttribute(new Attribute("namespace", "##other"));
            anyElement.addAttribute(new Attribute("processContents", "strict"));
            choiceElement.appendChild(anyElement);
        }
        choiceElement.addAttribute(new Attribute("maxOccurs", "unbounded"));
        rootType.appendChild(choiceElement);
    }

    public void addToResourceClass(final JDefinedClass resourceClass, final JMethod constructor) {
    }

    public void addToBuilderClass(final JDefinedClass builderClass) {
    }

    public void addToResolvedResourceClass(final JDefinedClass resolvedClass, final JMethod constructor) {
    }

    public void addToResolvedInterface(final JDefinedClass resolvedInterface) {
    }
}
