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

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;
import org.jboss.mgmt.AbstractResource;
import org.jboss.mgmt.AttributeValidator;
import org.jboss.mgmt.ExceptionThrowingValidationContext;
import org.jboss.mgmt.ModelNodeDeparser;
import org.jboss.mgmt.ModelNodeParser;
import org.jboss.mgmt.Resource;
import org.jboss.mgmt.VirtualAttribute;
import org.jboss.mgmt.annotation.Access;
import org.jboss.mgmt.annotation.xml.XmlRender;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JDocComment;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JType;
import com.sun.codemodel.JTypeVar;
import com.sun.codemodel.JVar;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import javax.tools.StandardLocation;

import static com.sun.codemodel.ClassType.*;
import static com.sun.codemodel.JMod.*;
import static javax.tools.Diagnostic.Kind.*;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class SessionImpl implements Session {

    private static final String XSD = "http://www.w3.org/2001/XMLSchema";
    private final ProcessingEnvironment env;
    private final List<RootResourceBuilderImpl> resources = new ArrayList<RootResourceBuilderImpl>();
    private final Map<String, Set<RootResourceBuilderImpl>> rootByNamespace = new HashMap<String, Set<RootResourceBuilderImpl>>();

    SessionImpl(final ProcessingEnvironment env) {
        this.env = env;
    }

    public RootResourceBuilder rootResource(final String type, final DeclaredType resourceInterface, final String version) {
        final RootResourceBuilderImpl builder = new RootResourceBuilderImpl(this, type, resourceInterface);
        resources.add(builder);
        return builder;
    }

    void addNamespace(String namespace, RootResourceBuilderImpl impl) {
        Set<RootResourceBuilderImpl> set = rootByNamespace.get(namespace);
        if (set == null) {
            rootByNamespace.put(namespace, set = new HashSet<RootResourceBuilderImpl>());
        }
        set.add(impl);
    }

    public AttributeTypeBuilder attributeType(final DeclaredType attributeInterface) {
        return null;
    }

    public Session addXmlNamespace(final String xmlns, final String version, final String schemaLocation) {
        return null;
    }

    public Session generateSource() {

        // --------------------------------------------------
        // Utility instances
        // --------------------------------------------------

        final Filer filer = env.getFiler();
        final Messager messager = env.getMessager();
        final Types types = env.getTypeUtils();
        final Elements elements = env.getElementUtils();
        final JCodeModel codeModel = new JCodeModel();

        // --------------------------------------------------
        // Common types
        // --------------------------------------------------

        final JClass modelNodeParser = codeModel.ref(ModelNodeParser.class).erasure();
        final JClass modelNodeDeparser = codeModel.ref(ModelNodeDeparser.class).erasure();
        final JClass xmlStreamReader = codeModel.ref(XMLStreamReader.class);
        final JClass xmlStreamWriter = codeModel.ref(XMLStreamWriter.class);
        final JClass xmlStreamException = codeModel.ref(XMLStreamException.class);
        final JClass exceptionThrowingValidationContext = codeModel.ref(ExceptionThrowingValidationContext.class);
        final JClass attributeValidator = codeModel.ref(AttributeValidator.class);
        final JClass resourceJClass = codeModel.ref(Resource.class);
        final JClass abstractResource = codeModel.ref(AbstractResource.class);
        final JClass string = codeModel.ref(String.class);
        final JClass virtualAttribute = codeModel.ref(VirtualAttribute.class).erasure();

        for (RootResourceBuilderImpl resource : resources) {

            // --------------------------------------------------
            // Gather resource information
            // --------------------------------------------------

            final TypeElement resourceInterfaceElement = (TypeElement) ((DeclaredType) resource.getResourceInterface()).asElement();
            final String resourceFqcn = resourceInterfaceElement.getQualifiedName().toString();
            final String resourceScn = resourceInterfaceElement.getSimpleName().toString();
            if (! resourceFqcn.endsWith("Resource")) {
                messager.printMessage(ERROR, "Resource type " + resourceFqcn + " must end in \"...Resource\"", resourceInterfaceElement);
                continue;
            }

            final String fqcn = resourceFqcn.substring(0, resourceFqcn.length() - 8);
            final String scn = resourceScn.substring(0, resourceScn.length() - 8);
            final String xmlName = def(resource.getXmlName(), xmlify(scn));

            // --------------------------------------------------
            // Code model class instances
            // --------------------------------------------------

            final JDefinedClass parserClass;
            final JDefinedClass deparserClass;
            final JDefinedClass builderInterface;
            final JDefinedClass builderClass;
            final JDefinedClass implementationClass;
            final JDefinedClass coreClass;

            final JClass resourceInterface = (JClass) CodeModelUtils.typeFor(env, codeModel, resource.getResourceInterface());

            final Element xsdRoot;
            final Document xsdDocument;

            try {
                parserClass = codeModel._class(PUBLIC | FINAL, fqcn + "_ParserImpl", CLASS);
                deparserClass = codeModel._class(PUBLIC | FINAL, fqcn + "_DeparserImpl", CLASS);
                builderInterface = codeModel._class(PUBLIC, fqcn + "_Builder", INTERFACE);
                builderClass = codeModel._class(PUBLIC | FINAL, fqcn + "_BuilderImpl", CLASS);
                implementationClass = codeModel._class(PUBLIC | FINAL, fqcn + "_Impl", CLASS);
                coreClass = codeModel._class(PUBLIC | FINAL, fqcn, CLASS);
            } catch (JClassAlreadyExistsException e) {
                messager.printMessage(ERROR, "Duplicate class generation for " + fqcn);
                continue;
            }

            // --------------------------------------------------
            // Schema root element for root resource
            // --------------------------------------------------

            xsdRoot = new Element("schema");
            xsdDocument = new Document(xsdRoot);

            xsdRoot.addNamespaceDeclaration("xs", XSD);
            xsdRoot.setNamespaceURI("ext:the.prefix:1.0");
            xsdRoot.addAttribute(new Attribute("targetNamespace", "ext:the.prefix:1.0"));
            xsdRoot.addAttribute(new Attribute("elementFormDefault", "qualified"));
            xsdRoot.addAttribute(new Attribute("attributeFormDefault", "unqualified"));

            final Element elementElement = new Element("element", XSD);
            xsdRoot.appendChild(elementElement);
            elementElement.addAttribute(new Attribute("name", xmlName));
            elementElement.addAttribute(new Attribute("type", xmlName));

            final Element typeElement = new Element("complexType", XSD);
            xsdRoot.appendChild(typeElement);
            typeElement.addAttribute(new Attribute("name", xmlName));

            final Element typeSeqElement = new Element("sequence", XSD); // todo - xs:all?
            typeElement.appendChild(typeSeqElement);

            addDocumentation(elementElement, "RESOURCE DESCRIPTION");

            // --------------------------------------------------
            // Class headers, extends/implements
            // --------------------------------------------------

            parserClass.direct("/* GENERATED SOURCE - DO NOT EDIT */\n");
            deparserClass.direct("/* GENERATED SOURCE - DO NOT EDIT */\n");
            builderInterface.direct("/* GENERATED SOURCE - DO NOT EDIT */\n");
            builderClass.direct("/* GENERATED SOURCE - DO NOT EDIT */\n");
            implementationClass.direct("/* GENERATED SOURCE - DO NOT EDIT */\n");
            coreClass.direct("/* GENERATED SOURCE - DO NOT EDIT */\n");

            final JTypeVar builderInterfaceP = builderInterface.generify("P");
            builderInterface._extends(codeModel.ref(SubBuilder.class).narrow(builderInterfaceP));

            builderInterface.javadoc().add("A builder for " + resource.getName() + " resources.  This interface was automatically generated.");

            final JTypeVar builderClassP = builderClass.generify("P");
            builderClass._implements(builderInterface.narrow(builderClassP));

            parserClass._implements(modelNodeParser.narrow(resourceInterface));
            deparserClass._implements(modelNodeDeparser.narrow(resourceInterface));

            implementationClass._extends(abstractResource);
            implementationClass._implements(resourceInterface);

            final JMethod implConstructor = implementationClass.constructor(PUBLIC);
            final JBlock implConstructorBody = implConstructor.body();
            final JInvocation implConstructorSuperCall = implConstructorBody.invoke("super");
            implConstructorSuperCall.arg(implConstructor.param(FINAL, string, "preComment"));
            implConstructorSuperCall.arg(implConstructor.param(FINAL, string, "postComment"));
            implConstructorSuperCall.arg(implConstructor.param(FINAL, string, "name"));
            implConstructorSuperCall.arg(implConstructor.param(FINAL, resourceJClass, "parent"));

            // The core class is not instantiatable
            coreClass.constructor(PRIVATE);

            // --------------------------------------------------
            // Resource parse/deparse methods
            // --------------------------------------------------

            final JMethod parseMethod = parserClass.method(PUBLIC, resourceInterface, "parse");
            parseMethod._throws(xmlStreamException);
            final JVar parseMethodStreamReader = parseMethod.param(xmlStreamReader, "streamReader");
            final JBlock parseMethodBody = parseMethod.body();

            final JMethod deparseMethod = deparserClass.method(PUBLIC, codeModel.VOID, "deparse");
            deparseMethod._throws(xmlStreamException);
            final JVar deparseMethodStreamWriter = deparseMethod.param(xmlStreamWriter, "streamWriter");
            final JVar deparseMethodResource = deparseMethod.param(resourceInterface, "resource");
            final JBlock deparseMethodBody = deparseMethod.body();

            for (AttributeBuilderImpl<?> attributeBuilder : resource.getAttributes()) {

                // --------------------------------------------------
                // Gather information about the attribute
                // --------------------------------------------------

                final XmlRender.As as = attributeBuilder.getXmlRenderAs();
                final boolean required = attributeBuilder.isRequired();
                final String defaultValue = attributeBuilder.getDefaultValue();
                final Access access = attributeBuilder.getAccess();
                final List<DeclaredType> validators = attributeBuilder.getValidators();
                final DeclaredType virtual = attributeBuilder.getVirtual();

                final String name = attributeBuilder.getName();
                final String getterName;
                final String fieldName = "attr_" + name;
                final String attrXmlName = def(attributeBuilder.getXmlName(), xmlify(name));

                final TypeMirror attributeType = attributeBuilder.getType();
                final JType attributeJType = CodeModelUtils.typeFor(env, codeModel, attributeType);

                final JExpression defaultValueExpr = defaultValue == null ? null : JExpr._null(); // todo convert defaultValue

                if (attributeType.equals(types.getPrimitiveType(TypeKind.BOOLEAN))) {
                    getterName = "is" + name;
                } else {
                    getterName = "get" + name;
                }

                // --------------------------------------------------
                // Add to XSD
                // --------------------------------------------------

                if (as == XmlRender.As.ELEMENT) {
                    final Element elem = new Element("element");
                    typeSeqElement.appendChild(elem);
                    elem.addAttribute(new Attribute("name", attrXmlName));
                    elem.addAttribute(new Attribute("type", "???")); // todo XML type
                    elem.addAttribute(new Attribute("minOccurs", required ? "1" : "0"));
                    elem.addAttribute(new Attribute("maxOccurs", "1"));
                    addDocumentation(elem, attributeBuilder.getRootDescription());
                } else {
                    assert as == XmlRender.As.ATTRIBUTE;
                    final Element attr = new Element("attribute");
                    elementElement.appendChild(attr);
                    attr.addAttribute(new Attribute("name", attrXmlName));
                    attr.addAttribute(new Attribute("type", "???"));// todo XML type
                    attr.addAttribute(new Attribute("use", required ? "required" : "optional"));
                    addDocumentation(attr, attributeBuilder.getRootDescription());
                }

                // --------------------------------------------------
                // Generate impl class field, getter, & constructor param
                // --------------------------------------------------

                final JMethod getterMethod = implementationClass.method(PUBLIC, attributeJType, getterName);
                final JBlock getterMethodBody = getterMethod.body();
                if (access.isReadable()) {
                    if (virtual != null) {
                        // todo - cache virtual instances
                        getterMethodBody._return(JExpr.invoke(JExpr._new(virtualAttribute.narrow(attributeJType)), "getValue"));
                    } else {
                        final JFieldVar field = implementationClass.field(PRIVATE | FINAL, attributeJType, fieldName);
                        implConstructorBody.assign(field, implConstructor.param(FINAL, attributeJType, fieldName));
                        getterMethodBody._return(field);
                    }
                } else {
                    getterMethodBody._throw(abstractResource.staticInvoke("notReadable"));
                }

                // --------------------------------------------------
                // Generate builder field and setter method
                // --------------------------------------------------

                if (access.isWritable()) {
                    final JFieldVar attributeField = builderClass.field(PRIVATE, attributeJType, fieldName);
                    if (defaultValueExpr != null) {
                        attributeField.assign(defaultValueExpr);
                    }

                    final JMethod builderInterfaceSetMethod = builderInterface.method(NONE, builderInterface, name);
                    final JMethod builderClassSetMethod = builderClass.method(PUBLIC, builderInterface, name);

                    final JDocComment javadoc = builderInterfaceSetMethod.javadoc();

                    javadoc.add("Set the " + attributeBuilder.getRootDescription() + " for this resource.");
                    if (required) {
                        javadoc.add("  This attribute is required.");
                    } else {
                        javadoc.add("  This attribute is optional.");
                        if (defaultValue != null) {
                            javadoc.add("  If not specified, this attribute will default to \"" + defaultValue + "\".");
                        }
                    }
                    javadoc.addParam(name).append("the " + attributeBuilder.getRootDescription());
                    javadoc.addReturn().append("this builder");

                    // todo for complex types, we must deconstruct the type and create a method param for each component...
                    builderInterfaceSetMethod.param(attributeJType, name);
                    final JVar newValueParam = builderClassSetMethod.param(attributeJType, name);

                    final JBlock body = builderClassSetMethod.body();
                    if (! validators.isEmpty()) {

                        // --------------------------------------------------
                        // Emit validation block
                        // --------------------------------------------------

                        final JVar context = body.decl(FINAL, exceptionThrowingValidationContext, "_context", JExpr._new(exceptionThrowingValidationContext));
                        for (DeclaredType validator : validators) {
                            final JClass validatorType = (JClass) CodeModelUtils.typeFor(env, codeModel, validator);
                            // todo - cache validator instances
                            final JInvocation inv = body.invoke(JExpr._new(validatorType.erasure().narrow(resourceInterface, attributeJType.boxify())), "validate");
                            inv.arg(JExpr._null()); // TODO - no resource yet...?
                            inv.arg(JExpr.lit(name));
                            inv.arg(JExpr._null());
                            inv.arg(newValueParam);
                            inv.arg(context);
                        }
                        body.invoke(context, "throwProblems");
                    }

                    body.assign(attributeField, newValueParam);
                    body._return(JExpr._this());
                }
            }

            final Serializer serializer;
            final OutputStream stream;
            try {
                stream = filer.createResource(StandardLocation.SOURCE_OUTPUT, "", "META-INF/" + xmlName + "_X_X.xsd").openOutputStream();
                try {
                    serializer = new Serializer(stream);
                    serializer.write(xsdDocument);
                    serializer.flush();
                } finally {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        messager.printMessage(ERROR, "Failed to close XSD stream: " + e);
                    }
                }
            } catch (IOException e) {
                messager.printMessage(ERROR, "Failed to write XSD: " + e);
            }
        }
        // emit
        try {
            codeModel.build(new FilerCodeWriter(filer));
        } catch (IOException e) {
            messager.printMessage(ERROR, "Failed to write source file: " + e);
        }
        return this;
    }

    private static void addDocumentation(final Element element, final String documentation) {
        final Element annotation = new Element("annotation", XSD);
        element.appendChild(annotation);
        final Element documentationElement = new Element("documentation", XSD);
        annotation.appendChild(documentationElement);
        documentationElement.appendChild(documentation);
    }

    private static <T> T def(T test, T def) {
        return test == null ? def : test;
    }

    /*
      AFunnyXMLKindAThing -> a-funny-xml-kind-a-thing
     */
    private static String xmlify(String camelHumpsName) {
        final int length = camelHumpsName.length();
        final StringBuilder builder = new StringBuilder(length + length >> 1);
        int idx = 0;
        int c = camelHumpsName.codePointAt(idx), n;
        boolean wordDone = false;
        for (;;) {
            idx = camelHumpsName.offsetByCodePoints(idx, 1);
            if (idx < length) {
                n = camelHumpsName.codePointAt(idx);
                if (Character.isLowerCase(c) && Character.isUpperCase(n)) {
                    builder.appendCodePoint(c);
                    wordDone = true;
                } else if (Character.isUpperCase(c) && Character.isLowerCase(n) || wordDone) {
                    builder.append('-');
                    builder.appendCodePoint(Character.toLowerCase(c));
                    wordDone = false;
                } else {
                    builder.appendCodePoint(Character.toLowerCase(c));
                }
                c = n;
                continue;
            } else {
                builder.appendCodePoint(Character.toLowerCase(c));
                return builder.toString();
            }
        }
    }

    public Session generateSchema() {
        return null;
    }
}
