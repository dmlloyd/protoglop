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
import org.jboss.jdeparser.JDeparser;
import org.jboss.jdeparser.JExpr;
import org.jboss.jdeparser.JMethod;
import org.wildfly.core.management.ResourceBuilderFactory;
import org.wildfly.core.management.RootNodeBuilder;
import org.wildfly.core.management.ResourceBuilderFactory;

import javax.lang.model.element.TypeElement;

import static org.jboss.jdeparser.ClassType.CLASS;
import static org.jboss.jdeparser.JMod.FINAL;
import static org.jboss.jdeparser.JMod.PUBLIC;
import static org.wildfly.core.management.processor.SchemaInfo.XS;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class RootResourceInfo {

    private final TypeElement resourceInterface;
    private final String name;
    private final String type;
    private final String[] provides;
    private final String xmlName;
    private final ResourceInfo resourceInfo;

    RootResourceInfo(final TypeElement resourceInterface, final String name, final String type, final String[] provides, final String xmlName, final ResourceInfo resourceInfo) {
        this.resourceInterface = resourceInterface;
        this.name = name;
        this.type = type;
        this.provides = provides;
        this.xmlName = xmlName;
        this.resourceInfo = resourceInfo;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String[] getProvides() {
        return provides;
    }

    public String getXmlName() {
        return xmlName;
    }

    public ResourceInfo getResourceInfo() {
        return resourceInfo;
    }

    public void addToSchema(SchemaGeneratorContext ctxt) {
        final Element rootElementDefinition = ctxt.addRootElement(xmlName, this, resourceInterface);
        if (rootElementDefinition == null) return;
        final Element typeElement = new Element("xs:complexType", SchemaInfo.XS);
        rootElementDefinition.addAttribute(new Attribute("name", xmlName));
        rootElementDefinition.appendChild(typeElement);
        GeneratorUtils.addDocumentation(rootElementDefinition, "*** DOCUMENTATION HERE ***");
        final Element seqElement = new Element("xs:sequence", SchemaInfo.XS);
        typeElement.appendChild(seqElement);
        resourceInfo.addToSchemaType(ctxt, seqElement, typeElement);
    }

    public void generateClasses(GeneratorContext ctxt) {
        if (ctxt.generated(this)) {
            return;
        }
        final JDeparser deparser = ctxt.getDeparser();
        final String builderName = resourceInterface.getQualifiedName().toString() + "Builder";
        final String factoryName = builderName + "Factory";
        final JDefinedClass factoryClass = deparser._class(PUBLIC | FINAL, factoryName, CLASS);
        final JDefinedClass builderClass = deparser._class(PUBLIC | FINAL, builderName, CLASS);

        factoryClass._implements(deparser.ref(ResourceBuilderFactory.class).narrow(deparser.ref(resourceInterface.getQualifiedName().toString()), builderClass));

        final JMethod createNewMethod = factoryClass.method(PUBLIC | FINAL, builderClass, "createNew");
        createNewMethod.body()._return(JExpr._new(builderClass));

        builderClass._extends(deparser.ref(RootNodeBuilder.class).narrow(deparser.ref(resourceInterface.getQualifiedName().toString())));
        resourceInfo.generateClasses(ctxt, builderClass);
    }
}
