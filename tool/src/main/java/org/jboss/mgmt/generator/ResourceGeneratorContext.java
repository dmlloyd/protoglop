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

import nu.xom.Element;
import org.jboss.mgmt.AbstractResource;
import org.jboss.mgmt.ModelNodeDeparser;
import org.jboss.mgmt.ModelNodeParser;
import org.jboss.mgmt.NestedBuilder;
import org.jboss.mgmt.ResourceNode;

import org.jboss.jdeparser.JBlock;
import org.jboss.jdeparser.JClass;
import org.jboss.jdeparser.JClassAlreadyExistsException;
import org.jboss.jdeparser.JDeparser;
import org.jboss.jdeparser.JDefinedClass;
import org.jboss.jdeparser.JInvocation;
import org.jboss.jdeparser.JMethod;
import org.jboss.jdeparser.JTypeVar;

import javax.tools.Diagnostic;

import static org.jboss.jdeparser.ClassType.CLASS;
import static org.jboss.jdeparser.ClassType.INTERFACE;
import static org.jboss.jdeparser.JMod.FINAL;
import static org.jboss.jdeparser.JMod.PUBLIC;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ResourceGeneratorContext {

    private final GeneratorContext context;
    private final ResourceInfo resourceInfo;
    private boolean classesGenerated;

    public ResourceGeneratorContext(final GeneratorContext context, final ResourceInfo resourceInfo) {
        this.context = context;
        this.resourceInfo = resourceInfo;
    }

    public void generateClasses() {
        if (classesGenerated) return;
        classesGenerated = true;
        final JDeparser deparser = context.getDeparser();
        final String interfaceName = resourceInfo.getTypeElement().getQualifiedName().toString();
        JClass resourceInterface = deparser.ref(interfaceName);
        try {
            JDefinedClass resourceImplClass = deparser._class(PUBLIC | FINAL, interfaceName + "ResourceImpl", CLASS);
            JDefinedClass parserClass = deparser._class(PUBLIC | FINAL, interfaceName + "ParserImpl", CLASS);
            JDefinedClass deparserClass = deparser._class(PUBLIC | FINAL, interfaceName + "DeparserImpl", CLASS);
            JDefinedClass builderInterface = deparser._class(PUBLIC, interfaceName + "Builder", INTERFACE);
            JDefinedClass builderClass = deparser._class(PUBLIC | FINAL, interfaceName + "BuilderImpl", CLASS);
            JDefinedClass resourceNodeClass = deparser._class(PUBLIC | FINAL, interfaceName + "NodeImpl", CLASS);

            final JMethod resourceImplConstructor = resourceImplClass.constructor(PUBLIC);
            final JBlock implConstructorBody = resourceImplConstructor.body();
            final JInvocation implConstructorSuperCall = implConstructorBody.invoke("super");
            implConstructorSuperCall.arg(resourceImplConstructor.param(FINAL, deparser.ref(String.class), "preComment"));
            implConstructorSuperCall.arg(resourceImplConstructor.param(FINAL, deparser.ref(String.class), "postComment"));
            implConstructorSuperCall.arg(resourceImplConstructor.param(FINAL, deparser.ref(String.class), "name"));
            implConstructorSuperCall.arg(resourceImplConstructor.param(FINAL, deparser.ref(ResourceNode.class).narrow(deparser.wildcard()), "parent"));
            final JBlock resourceImplConstructorInitBlock = implConstructorBody.block();
            final JBlock resourceImplConstructorPostConstructBlock = implConstructorBody.block();

            final JTypeVar builderInterfaceP = builderInterface.generify("P");
            builderInterface._extends(deparser.ref(NestedBuilder.class).erasure().erasure().narrow(builderInterfaceP));

            final JTypeVar builderClassP = builderClass.generify("P");
            builderClass._implements(builderInterface.narrow(builderClassP));

            parserClass._implements(deparser.ref(ModelNodeParser.class).erasure().narrow(builderInterface.narrow(deparser.wildcard())));
            deparserClass._implements(deparser.ref(ModelNodeDeparser.class).erasure().narrow(resourceInterface));

            resourceImplClass._extends(deparser.ref(AbstractResource.class));
            resourceImplClass._implements(resourceInterface);

            resourceNodeClass._extends(deparser.ref(ResourceNode.class).erasure().narrow(resourceInterface));
        } catch (JClassAlreadyExistsException e) {
            context.getEnv().getMessager().printMessage(Diagnostic.Kind.ERROR, "Duplicated class created: " + e);
        }
    }

    public void addToSchema(final SchemaGeneratorContext ctxt, final Element type) {
        final Element seq = new Element("xs:sequence", SchemaInfo.XS);
        for (ResourceMember member : resourceInfo.getResourceMembers()) {
            member.addToSchema(ctxt, type, seq);
        }
    }

    public GeneratorContext getContext() {
        return context;
    }

    public ResourceInfo getResourceInfo() {
        return resourceInfo;
    }

    public String getResourceXmlType() {
        return NameUtils.xmlify(resourceInfo.getName()) + "-type";
    }
}
