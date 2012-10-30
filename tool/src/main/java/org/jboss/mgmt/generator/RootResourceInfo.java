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
import org.jboss.mgmt.BuilderFactory;

import org.jboss.jdeparser.JClassAlreadyExistsException;
import org.jboss.jdeparser.JDeparser;
import org.jboss.jdeparser.JDefinedClass;
import org.jboss.jdeparser.JTypeVar;

import javax.tools.Diagnostic;

import static org.jboss.jdeparser.ClassType.CLASS;
import static org.jboss.jdeparser.JMod.FINAL;
import static org.jboss.jdeparser.JMod.PUBLIC;
import static org.jboss.mgmt.generator.GeneratorUtils.XSD;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class RootResourceInfo {
    private final String name;
    private final String type;
    private final String[] provides;
    private final String xmlName;
    private final ResourceInfo resourceInfo;

    RootResourceInfo(final String name, final String type, final String[] provides, final String xmlName, final ResourceInfo resourceInfo) {
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

    public void generate(final SchemaGeneratorContext ctxt) {
        final JDeparser deparser = ctxt.getContext().getDeparser();

        final String interfaceName = resourceInfo.getTypeElement().getQualifiedName().toString();

        final JDefinedClass builderFactoryClass;
        try {
            builderFactoryClass = deparser._class(PUBLIC | FINAL, interfaceName + "BuilderFactory", CLASS);
        } catch (JClassAlreadyExistsException e) {
            ctxt.getContext().getEnv().getMessager().printMessage(Diagnostic.Kind.ERROR, "Builder factory class already exists for " + interfaceName, resourceInfo.getTypeElement());
            return;
        }
        final JTypeVar builderFactoryP = builderFactoryClass.generify("P");
        builderFactoryClass._implements(deparser.ref(BuilderFactory.class).erasure().narrow(builderFactoryP, deparser.ref(interfaceName + "Builder").narrow(builderFactoryP)));

        final Element rootElementDefinition = new Element("xs:element", XSD);
        rootElementDefinition.addAttribute(new Attribute("name", xmlName));
        GeneratorUtils.addDocumentation(rootElementDefinition, "*** DOCUMENTATION HERE ***");
        ctxt.addRootElement(name, rootElementDefinition);
        final ResourceGeneratorContext resourceGeneratorContext = ResourceGeneratorContext.create(ctxt, this, null);
        resourceInfo.generate(resourceGeneratorContext);
        final Element type = new Element("xs:complexType", XSD);
        final Element seq = new Element("xs:sequence", XSD);
        for (Element nestedElement : resourceGeneratorContext.getNestedElements()) {
            seq.appendChild(nestedElement);
        }
        type.appendChild(seq);
        for (Element xmlAttributeDefinition : resourceGeneratorContext.getXmlAttributeDefinitions()) {
            type.appendChild(xmlAttributeDefinition);
        }
        rootElementDefinition.appendChild(type);
    }
}
