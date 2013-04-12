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
import org.jboss.jdeparser.JClass;
import org.jboss.jdeparser.JClassAlreadyExistsException;
import org.jboss.jdeparser.JDefinedClass;
import org.jboss.jdeparser.JDeparser;
import org.jboss.jdeparser.JExpr;
import org.jboss.jdeparser.JMethod;
import org.jboss.jdeparser.JVar;
import org.jboss.mgmt.AbstractResource;
import org.jboss.mgmt.Resource;
import org.jboss.mgmt.ResourceNode;
import org.jboss.mgmt.XMLParseException;

import javax.xml.stream.XMLStreamReader;

import javax.lang.model.element.TypeElement;

import javax.tools.Diagnostic;

import static org.jboss.jdeparser.ClassType.CLASS;
import static org.jboss.jdeparser.ClassType.INTERFACE;
import static org.jboss.jdeparser.JMod.FINAL;
import static org.jboss.jdeparser.JMod.PROTECTED;
import static org.jboss.jdeparser.JMod.PUBLIC;

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

    public void generateClasses(final GeneratorContext ctxt, final JDefinedClass builderClass) {
        if (ctxt.generated(this)) {
            return;
        }
        final JDeparser deparser = ctxt.getDeparser();

        // Original resource
        final String resourceName = typeElement.getQualifiedName().toString();
        final JClass resource = deparser.ref(resourceName);

        // Resolved interface
        final String resolvedResourceName = resourceName + "Resolved";
        final JDefinedClass resolvedResource;

        // Resource class
        final String resourceClassName = resourceName + "Impl";
        final JDefinedClass resourceClass;

        // Resource node proxy class
        final String resourceNodeClassName = resourceName + "NodeImpl";


        // Resolved resource class
        final String resolvedResourceClassName = resolvedResourceName + "Impl";
        final JDefinedClass resolvedResourceClass;

        resolvedResource = deparser._class(PUBLIC, resolvedResourceName, INTERFACE);
        resourceClass = deparser._class(FINAL, resourceClassName, CLASS);
        resolvedResourceClass = deparser._class(FINAL, resolvedResourceClassName, CLASS);

        // Resolved interface
        resolvedResource._extends(Resource.class);

        // Resource class
        resourceClass._extends(AbstractResource.class)._implements(resource);

        final JClass anyResourceNode = deparser.ref(ResourceNode.class).erasure().narrow(deparser.wildcard());

        final JMethod resourceConstructor = resourceClass.constructor(0);
        {
            final JVar nodeParam = resourceConstructor.param(FINAL, anyResourceNode, "node");
            final JVar nameParam = resourceConstructor.param(FINAL, String.class, "name");
            final JVar preCommentParam = resourceConstructor.param(FINAL, String.class, "preComment");
            final JVar postCommentParam = resourceConstructor.param(FINAL, String.class, "postComment");
            resourceConstructor.body().invoke("super").arg(nameParam).arg(preCommentParam).arg(postCommentParam).arg(nodeParam.invoke("getParent"));
        }

        // Resolved resource class
        resolvedResourceClass._extends(AbstractResource.class)._implements(resolvedResource);

        final JMethod resolvedResourceConstructor = resolvedResourceClass.constructor(0);
        {
            final JVar nameParam = resolvedResourceConstructor.param(FINAL, String.class, "name");
            final JVar preCommentParam = resolvedResourceConstructor.param(FINAL, String.class, "preComment");
            final JVar postCommentParam = resolvedResourceConstructor.param(FINAL, String.class, "postComment");
            final JVar parentParam = resolvedResourceConstructor.param(FINAL, anyResourceNode, "parent");
            resolvedResourceConstructor.body().invoke("super").arg(nameParam).arg(preCommentParam).arg(postCommentParam).arg(parentParam);
        }

        // Builder class

        builderClass.constructor(0);

        final JMethod resourceBuilderConstruct = builderClass.method(PROTECTED | FINAL, deparser.ref(ResourceNode.class).erasure().narrow(resource), "construct");
        resourceBuilderConstruct.param(deparser.ref(ResourceNode.class).erasure().narrow(deparser.wildcard()), "parent");
        resourceBuilderConstruct.body()._return(JExpr._null());

        final JMethod fromXmlMethod = builderClass.method(PUBLIC | FINAL, deparser.VOID, "fromXml");
        fromXmlMethod.param(FINAL, XMLStreamReader.class, "reader");
        fromXmlMethod._throws(XMLParseException.class);
        fromXmlMethod.body();

        for (ResourceMember resourceMember : resourceMembers) {
            resourceMember.addToBuilderClass(builderClass);
            resourceMember.addToResourceClass(resourceClass, resourceConstructor);
            resourceMember.addToResolvedInterface(resolvedResource);
            resourceMember.addToResolvedResourceClass(resolvedResourceClass, resolvedResourceConstructor);
        }
    }
}
