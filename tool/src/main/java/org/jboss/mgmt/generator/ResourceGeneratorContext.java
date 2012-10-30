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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import nu.xom.Element;
import org.jboss.mgmt.AbstractResource;
import org.jboss.mgmt.BuilderFactory;
import org.jboss.mgmt.ModelNodeDeparser;
import org.jboss.mgmt.ModelNodeParser;
import org.jboss.mgmt.NestedBuilder;
import org.jboss.mgmt.ResourceNode;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JTypeVar;

import javax.tools.Diagnostic;

import static com.sun.codemodel.ClassType.CLASS;
import static com.sun.codemodel.ClassType.INTERFACE;
import static com.sun.codemodel.JMod.FINAL;
import static com.sun.codemodel.JMod.PUBLIC;

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
    private final JDefinedClass parserClass;
    private final JDefinedClass deparserClass;
    private final JDefinedClass builderInterface;
    private final JDefinedClass builderClass;
    private final JDefinedClass resourceNodeClass;
    private final JMethod resourceImplConstructor;
    private final JBlock resourceImplConstructorInitBlock;
    private final JBlock resourceImplConstructorPostConstructBlock;

    private ResourceGeneratorContext(final SchemaGeneratorContext context, final RootResourceInfo resourceInfo, final ResourceGeneratorContext parent, final JClass resourceInterface, final JDefinedClass resourceImplClass, final JDefinedClass parserClass, final JDefinedClass deparserClass, final JDefinedClass builderInterface, final JDefinedClass builderClass, final JDefinedClass resourceNodeClass) {
        this.context = context;
        this.resourceInfo = resourceInfo;
        this.parent = parent;
        this.resourceInterface = resourceInterface;
        this.resourceImplClass = resourceImplClass;
        this.parserClass = parserClass;
        this.deparserClass = deparserClass;
        this.builderInterface = builderInterface;
        this.builderClass = builderClass;
        this.resourceNodeClass = resourceNodeClass;

        final JCodeModel codeModel = context.getContext().getCodeModel();

        final JMethod resourceImplConstructor = resourceImplClass.constructor(PUBLIC);
        final JBlock implConstructorBody = resourceImplConstructor.body();
        final JInvocation implConstructorSuperCall = implConstructorBody.invoke("super");
        implConstructorSuperCall.arg(resourceImplConstructor.param(FINAL, codeModel.ref(String.class), "preComment"));
        implConstructorSuperCall.arg(resourceImplConstructor.param(FINAL, codeModel.ref(String.class), "postComment"));
        implConstructorSuperCall.arg(resourceImplConstructor.param(FINAL, codeModel.ref(String.class), "name"));
        implConstructorSuperCall.arg(resourceImplConstructor.param(FINAL, codeModel.ref(ResourceNode.class).narrow(codeModel.wildcard()), "parent"));
        final JBlock resourceImplConstructorInitBlock = implConstructorBody.block();
        final JBlock resourceImplConstructorPostConstructBlock = implConstructorBody.block();

        final JTypeVar builderInterfaceP = builderInterface.generify("P");
        builderInterface._extends(codeModel.ref(NestedBuilder.class).erasure().erasure().narrow(builderInterfaceP));

        final JTypeVar builderClassP = builderClass.generify("P");
        builderClass._implements(builderInterface.narrow(builderClassP));

        parserClass._implements(codeModel.ref(ModelNodeParser.class).erasure().narrow(builderInterface.narrow(codeModel.wildcard())));
        deparserClass._implements(codeModel.ref(ModelNodeDeparser.class).erasure().narrow(resourceInterface));

        resourceImplClass._extends(codeModel.ref(AbstractResource.class));
        resourceImplClass._implements(resourceInterface);

        resourceNodeClass._extends(codeModel.ref(ResourceNode.class).erasure().narrow(resourceInterface));

        this.resourceImplConstructor = resourceImplConstructor;
        this.resourceImplConstructorInitBlock = resourceImplConstructorInitBlock;
        this.resourceImplConstructorPostConstructBlock = resourceImplConstructorPostConstructBlock;
    }

    public static ResourceGeneratorContext create(final SchemaGeneratorContext context, final RootResourceInfo resourceInfo, final ResourceGeneratorContext parent) {
        final JCodeModel codeModel = context.getContext().getCodeModel();
        final String interfaceName = resourceInfo.getResourceInfo().getTypeElement().getQualifiedName().toString();
        JClass resourceInterface = codeModel._getClass(interfaceName);
        try {
            JDefinedClass resourceImplClass = codeModel._class(PUBLIC | FINAL, interfaceName + "ResourceImpl", CLASS);
            JDefinedClass parserClass = codeModel._class(PUBLIC | FINAL, interfaceName + "ParserImpl", CLASS);
            JDefinedClass deparserClass = codeModel._class(PUBLIC | FINAL, interfaceName + "DeparserImpl", CLASS);
            JDefinedClass builderInterface = codeModel._class(PUBLIC, interfaceName + "Builder", INTERFACE);
            JDefinedClass builderClass = codeModel._class(PUBLIC | FINAL, interfaceName + "BuilderImpl", CLASS);
            JDefinedClass resourceNodeClass = codeModel._class(PUBLIC | FINAL, interfaceName + "NodeImpl", CLASS);

            return new ResourceGeneratorContext(context, resourceInfo, parent, resourceInterface, resourceImplClass, parserClass, deparserClass, builderInterface, builderClass, resourceNodeClass);
        } catch (JClassAlreadyExistsException e) {
            context.getContext().getEnv().getMessager().printMessage(Diagnostic.Kind.ERROR, "Duplicated class created: " + e);
            return null;
        }
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

    public JBlock getResourceImplConstructorPostConstructBlock() {
        return resourceImplConstructorPostConstructBlock;
    }

    public Map<String, Element> getAttributeDefinitions() {
        return attributeDefinitions;
    }

    public JDefinedClass getParserClass() {
        return parserClass;
    }

    public JDefinedClass getDeparserClass() {
        return deparserClass;
    }

    public JDefinedClass getBuilderInterface() {
        return builderInterface;
    }

    public JDefinedClass getBuilderClass() {
        return builderClass;
    }

    public JDefinedClass getResourceNodeClass() {
        return resourceNodeClass;
    }
}
