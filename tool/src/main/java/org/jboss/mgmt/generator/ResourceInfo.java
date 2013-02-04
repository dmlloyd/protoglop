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

import nu.xom.Attribute;
import nu.xom.Element;

import javax.lang.model.element.TypeElement;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ResourceInfo {

    private final TypeElement typeElement;
    private final ResourceMember[] resourceMembers;
    private final String name;
    private final String xmlName;
    private final String xmlTypeName;

    ResourceInfo(final TypeElement typeElement, final ResourceMember[] resourceMembers, final String name, final String xmlName, final String xmlTypeName) {
        this.typeElement = typeElement;
        this.resourceMembers = resourceMembers;
        this.name = name;
        this.xmlName = xmlName;
        this.xmlTypeName = xmlTypeName;
    }

    public TypeElement getTypeElement() {
        return typeElement;
    }

    public ResourceMember[] getResourceMembers() {
        return resourceMembers;
    }

    public void addToSchema(final SchemaGeneratorContext ctxt) {
        final Element element = ctxt.addRootComplexType(xmlTypeName, this, typeElement);
        if (element == null) return;
        final Element seqElement = new Element("xs:sequence", SchemaInfo.XS);
        element.appendChild(seqElement);
        addToSchemaType(ctxt, seqElement, element);
    }

    public void addToSchemaType(final SchemaGeneratorContext ctxt, final Element enclosingSeqElement, final Element enclosingTypeElement) {
        final Element nameAttribute = new Element("xs:attribute", SchemaInfo.XS);
        nameAttribute.addAttribute(new Attribute("name", "name"));
        nameAttribute.addAttribute(new Attribute("type", "xs:string"));
        nameAttribute.addAttribute(new Attribute("use", "required"));
        enclosingTypeElement.appendChild(nameAttribute);
        for (ResourceMember member : resourceMembers) {
            member.addToSchema(ctxt, enclosingTypeElement, enclosingSeqElement);
        }
    }

    public String getName() {
        return name;
    }

    public String getXmlTypeName() {
        return xmlTypeName;
    }

    public String getXmlName() {
        return xmlName;
    }
}
