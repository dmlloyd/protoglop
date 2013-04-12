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
import org.jboss.jdeparser.JDefinedClass;
import org.jboss.jdeparser.JMethod;

import javax.lang.model.element.TypeElement;

import static org.jboss.mgmt.generator.SchemaInfo.XS;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class AttributeGroupInfo implements ResourceMember {

    private final String xmlName;
    private final String xmlTypeName;
    private final TypeElement typeElement;
    private final ResourceMember[] members;

    public AttributeGroupInfo(final String xmlName, final String xmlTypeName, final TypeElement typeElement, final ResourceMember[] members) {
        this.xmlName = xmlName;
        this.xmlTypeName = xmlTypeName;
        this.typeElement = typeElement;
        this.members = members;
    }

    public TypeElement getTypeElement() {
        return typeElement;
    }

    public ResourceMember[] getMembers() {
        return members;
    }

    public void addToSchema(final SchemaGeneratorContext ctxt, final Element typeElement, final Element seqElement) {
        final Element elementElement = new Element("xs:element", XS);
        elementElement.addAttribute(new Attribute("name", xmlName));
        elementElement.addAttribute(new Attribute("type", xmlTypeName));
        seqElement.appendChild(elementElement);
        final Element groupTypeElement = ctxt.addRootComplexType(xmlTypeName, this, getTypeElement());
        if (groupTypeElement == null) {
            // already added
            return;
        }
        GeneratorUtils.addDocumentation(groupTypeElement, "** add group doc here **"); // todo
        final Element subSeqElement = new Element("xs:sequence", XS);
        for (ResourceMember member : members) {
            member.addToSchema(ctxt, groupTypeElement, subSeqElement);
        }
        groupTypeElement.appendChild(subSeqElement);
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
