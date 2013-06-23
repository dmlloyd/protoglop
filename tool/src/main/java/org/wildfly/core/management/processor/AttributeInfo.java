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
import org.wildfly.core.management.Access;
import org.wildfly.core.management.annotation.XmlRender;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class AttributeInfo implements ResourceMember {

    private final ExecutableElement executableElement;
    /**
     * Attribute names must always be stored using OriginalUpperCamelCase in order
     * to preserve the original case of acronym CamelWords.
     */
    private final String name;
    private final AttributeValueInfo valueInfo;
    private final Access access;
    private final TypeMirror virtual;
    private final boolean required;
    private final VariableElement defaultVal;
    private final TypeMirror[] validators;
    private final String xmlName;
    private final boolean wrapperElement;
    private final XmlRender.As renderAs;

    public AttributeInfo(final ExecutableElement executableElement, final String name, final AttributeValueInfo valueInfo, final Access access, final TypeMirror virtual, final boolean required, final VariableElement defaultVal, final TypeMirror[] validators, final String xmlName, final boolean wrapperElement, final XmlRender.As renderAs) {
        this.executableElement = executableElement;
        this.name = name;
        this.valueInfo = valueInfo;
        this.access = access;
        this.virtual = virtual;
        this.required = required;
        this.defaultVal = defaultVal;
        this.validators = validators;
        this.xmlName = xmlName;
        this.wrapperElement = wrapperElement;
        this.renderAs = renderAs;
    }

    public ExecutableElement getExecutableElement() {
        return executableElement;
    }

    public String getName() {
        return name;
    }

    public AttributeValueInfo getValueInfo() {
        return valueInfo;
    }

    public Access getAccess() {
        return access;
    }

    public TypeMirror getVirtual() {
        return virtual;
    }

    public boolean isRequired() {
        return required;
    }

    public VariableElement getDefaultVal() {
        return defaultVal;
    }

    public TypeMirror[] getValidators() {
        return validators;
    }

    public String getXmlName() {
        return xmlName;
    }

    public boolean isWrapperElement() {
        return wrapperElement;
    }

    public XmlRender.As getRenderAs() {
        return renderAs;
    }

    public void addToSchema(final SchemaGeneratorContext ctxt, final Element enclosingTypeElement, final Element enclosingSeqElement) {
        if (renderAs == XmlRender.As.ATTRIBUTE) {
            final Element attributeElement = new Element("xs:attribute", SchemaInfo.XS);
            attributeElement.addAttribute(new Attribute("name", xmlName));
            attributeElement.addAttribute(new Attribute("use", required ? "required" : "optional"));
            valueInfo.addToSchemaAsAttribute(this, enclosingSeqElement, enclosingTypeElement, attributeElement);
            GeneratorUtils.addDocumentation(attributeElement, "*** DOCS HERE ***");
            enclosingTypeElement.appendChild(attributeElement);
        } else {
            final Element elementElement = new Element("xs:element", SchemaInfo.XS);
            elementElement.addAttribute(new Attribute("name", xmlName));
            valueInfo.addToSchemaAsElement(this, enclosingSeqElement, enclosingTypeElement, elementElement);
        }
    }

    public void addToResourceClass(final JDefinedClass resourceClass, final JMethod constructor) {
        valueInfo.addToResourceClass(resourceClass, constructor);
    }

    public void addToBuilderClass(final JDefinedClass builderClass) {
        valueInfo.addToBuilderClass(builderClass);
    }

    public void addToResolvedResourceClass(final JDefinedClass resolvedClass, final JMethod constructor) {
        valueInfo.addToResolvedResourceClass(resolvedClass, constructor);
    }

    public void addToResolvedInterface(final JDefinedClass resolvedInterface) {
        valueInfo.addToResolvedInterface(resolvedInterface);
    }

    public void addToClass() {
//        final SchemaGeneratorContext schemaGeneratorContext = resourceGeneratorContext.getContext();
//        final GeneratorContext generatorContext = schemaGeneratorContext.getContext();
//        final ProcessingEnvironment env = generatorContext.getEnv();
//        final JDeparser deparser = generatorContext.getDeparser();
//
//        // ---------------------------
//        // Resource interface stuff
//        // ---------------------------
//
//        final JClass resourceInterface = resourceGeneratorContext.getResourceInterface();
//
//        final JType attributeJType = JDeparserUtils.typeFor(env, deparser, executableElement.getReturnType());
//        final String getterName = executableElement.getSimpleName().toString();
//        final String attrVarName = fieldify(name);
//
//        final JDefinedClass resourceImplClass = resourceGeneratorContext.getResourceImplClass();
//        final JMethod resourceImplConstructor = resourceGeneratorContext.getResourceImplConstructor();
//        final JBlock resourceImplConstructorInitBlock = resourceGeneratorContext.getResourceImplConstructorInitBlock();
//
//        final JMethod getterMethod = resourceImplClass.method(PUBLIC, attributeJType, getterName);
//        final JBlock getterMethodBody = getterMethod.body();
//        if (access.isReadable()) {
//
//        } else {
//            getterMethodBody._throw(deparser.ref(AbstractResource.class).staticInvoke("notReadable"));
//        }
//
//        // ---------------------------
//        // Builder class stuff
//        // ---------------------------
//
//        final JMethod setterDecl;
//        final JBlock setterBody;
    }
}
