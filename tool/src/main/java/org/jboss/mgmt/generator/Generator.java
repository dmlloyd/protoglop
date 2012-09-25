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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import nu.xom.Attribute;
import nu.xom.Comment;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;
import org.jboss.mgmt.AbstractResource;
import org.jboss.mgmt.BuilderFactory;
import org.jboss.mgmt.Entry;
import org.jboss.mgmt.ExceptionThrowingValidationContext;
import org.jboss.mgmt.ModelNodeDeparser;
import org.jboss.mgmt.ModelNodeParser;
import org.jboss.mgmt.NestedBuilder;
import org.jboss.mgmt.Resource;
import org.jboss.mgmt.VirtualAttribute;
import org.jboss.mgmt.annotation.Access;
import org.jboss.mgmt.annotation.RootResource;
import org.jboss.mgmt.annotation.xml.XmlRender;
import org.jboss.mgmt.ResourceNode;

import com.sun.codemodel.JArray;
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

import javax.annotation.Generated;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import javax.tools.StandardLocation;

import static com.sun.codemodel.ClassType.CLASS;
import static com.sun.codemodel.ClassType.INTERFACE;
import static com.sun.codemodel.JMod.FINAL;
import static com.sun.codemodel.JMod.NONE;
import static com.sun.codemodel.JMod.PRIVATE;
import static com.sun.codemodel.JMod.PUBLIC;
import static com.sun.codemodel.JMod.STATIC;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.WARNING;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class Generator {
    private static final String XSD = "http://www.w3.org/2001/XMLSchema";
    private static final String GENERATOR_VERSION = "1.0";

    private final ProcessingEnvironment env;
    private final RoundEnvironment roundEnv;
    private final Filer filer;
    private final Messager messager;
    private final Types types;
    private final Elements elements;
    private final SessionImpl session;
    private final JCodeModel codeModel = new JCodeModel();

    // --------------------------------------------------
    // Accumulated output
    // --------------------------------------------------

    static class DocInfo {
        final Map<String, Element> typeDecls = new TreeMap<String, Element>();
        final Map<String, Element> rootElementDecls = new TreeMap<String, Element>();
        final Set<String> altXmlNamespaces = new HashSet<String>();
        String schemaLocation;
        RootResource.Kind resourceNamespaceKind = RootResource.Kind.EXTENSION;
        String version;
        String namespace; // just the module part
    }

    private final Map<String, DocInfo> namespaceSchemas = new HashMap<String, DocInfo>();
    private final Set<String> generatedResourceTypes = new HashSet<String>();
    private final Set<String> generatedAttributeTypes = new HashSet<String>();
    private final Set<String> generatedAttributeGroups = new HashSet<String>();

    Generator(final ProcessingEnvironment env, final RoundEnvironment roundEnv, final SessionImpl session) {
        this.roundEnv = roundEnv;
        this.session = session;
        this.env = env;
        filer = env.getFiler();
        messager = env.getMessager();
        types = env.getTypeUtils();
        elements = env.getElementUtils();
    }

    void generate() {
        final List<RootResourceBuilderImpl> resources = session.getResources();

        for (RootResourceBuilderImpl resource : resources) {

            // --------------------------------------------------
            // Root-specific stuff
            // --------------------------------------------------

            final String version = def(resource.getVersion(), "1.0");
            final String namespace = def(resource.getNamespace(), "unspecified");
            final String schemaLocation = resource.getSchemaLocation();
            final RootResource.Kind resourceNamespaceKind = def(resource.getKind(), RootResource.Kind.EXTENSION);
            final String xmlNamespace = buildNamespace(resourceNamespaceKind, namespace, version);

            final DocInfo docInfo;
            if (! namespaceSchemas.containsKey(xmlNamespace)) {
                docInfo = new DocInfo();
                docInfo.schemaLocation = schemaLocation;
                docInfo.namespace = namespace;
                docInfo.resourceNamespaceKind = resourceNamespaceKind;
                docInfo.version = version;
                namespaceSchemas.put(xmlNamespace, docInfo);
            } else {
                docInfo = namespaceSchemas.get(xmlNamespace);
                if (! schemaLocation.equals(docInfo.schemaLocation)) {
                    messager.printMessage(WARNING, "Namespace '" + xmlNamespace + "' declared with conflicting schema locations");
                }
            }
            // Root resources declare elements in the root schema.
            new ResourceWriter(resource, docInfo, xmlNamespace, null).generate();
        }

        // --------------------------------------------------
        // Emit results (if no error)
        // --------------------------------------------------

        for (Map.Entry<String, DocInfo> entry : namespaceSchemas.entrySet()) {
            final Element schemaElement = new Element("xs:schema", XSD);
            final Document document = new Document(schemaElement);
            final String xmlNamespace = entry.getKey();
            final DocInfo docInfo = entry.getValue();

            schemaElement.addNamespaceDeclaration("", xmlNamespace);
            schemaElement.addNamespaceDeclaration("xs", XSD);

            schemaElement.addAttribute(new Attribute("targetNamespace", xmlNamespace));
            schemaElement.addAttribute(new Attribute("elementFormDefault", "qualified"));
            schemaElement.addAttribute(new Attribute("attributeFormDefault", "unqualified"));

            schemaElement.appendChild("\n");
            schemaElement.appendChild(new Comment("Root elements"));
            schemaElement.appendChild("\n");
            for (Map.Entry<String, Element> elementEntry : docInfo.rootElementDecls.entrySet()) {
                schemaElement.appendChild(elementEntry.getValue());
            }
            schemaElement.appendChild("\n");
            schemaElement.appendChild(new Comment("Element types"));
            schemaElement.appendChild("\n");
            for (Map.Entry<String, Element> elementEntry : docInfo.typeDecls.entrySet()) {
                schemaElement.appendChild(elementEntry.getValue());
            }
            if (docInfo.schemaLocation == null) {
                messager.printMessage(ERROR, "No namespace location for schema " + xmlNamespace);
                continue;
            }
            final URI uri;
            try {
                uri = new URI(docInfo.schemaLocation);
            } catch (URISyntaxException e) {
                messager.printMessage(ERROR, "Namespace schema location '" + docInfo.schemaLocation + "' is not valid for " + xmlNamespace);
                continue;
            }
            final String path = uri.getPath();
            if (path == null) {
                messager.printMessage(ERROR, "Namespace schema location '" + docInfo.schemaLocation + "' does not have a path component for " + xmlNamespace);
                continue;
            }
            final String fileName = path.substring(path.lastIndexOf('/') + 1);
            if (! fileName.endsWith(".xsd")) {
                messager.printMessage(WARNING, "Namespace schema location '" + docInfo.schemaLocation + "' should specify a file name ending in \".xsd\"");
            }
            if (! roundEnv.errorRaised()) {
                final Serializer serializer;
                final OutputStream stream;
                try {
                    stream = filer.createResource(StandardLocation.SOURCE_OUTPUT, "", "META-INF/" + fileName).openOutputStream();
                    try {
                        serializer = new Serializer(stream);
                        serializer.setIndent(4);
                        serializer.setLineSeparator("\n");
                        serializer.write(document);
                        serializer.flush();
                    } finally {
                        try {
                            stream.close();
                        } catch (IOException e) {
                            messager.printMessage(ERROR, "Failed to close XSD stream for '" + docInfo.schemaLocation + "' of " + xmlNamespace + ": " + e);
                        }
                    }
                } catch (IOException e) {
                    messager.printMessage(ERROR, "Failed to write XSD for '" + docInfo.schemaLocation + "' of " + xmlNamespace + ": " + e);
                }
            }
        }

        if (! roundEnv.errorRaised()) try {
            codeModel.build(new FilerCodeWriter(filer));
        } catch (IOException e) {
            messager.printMessage(ERROR, "Failed to write source files: " + e);
        }
    }

    class ResourceWriter {
        private final GeneralResourceBuilderImpl<?> resource;
        private final DocInfo docInfo;
        private final String xmlNamespace;
        private final Element parentType;

        ResourceWriter(final GeneralResourceBuilderImpl<?> resource, final DocInfo docInfo, final String xmlNamespace, final Element parentType) {
            this.resource = resource;
            this.docInfo = docInfo;
            this.xmlNamespace = xmlNamespace;
            this.parentType = parentType;
        }

        void generate() {

            // --------------------------------------------------
            // Gather resource information
            // --------------------------------------------------

            final TypeElement resourceInterfaceElement = (TypeElement) ((DeclaredType) resource.getResourceInterface()).asElement();
            final String resourceFqcn = resourceInterfaceElement.getQualifiedName().toString();
            final String resourceScn = resourceInterfaceElement.getSimpleName().toString();
            if (! resourceFqcn.endsWith("Resource")) {
                messager.printMessage(ERROR, "Resource type " + resourceFqcn + " must end in \"...Resource\"", resourceInterfaceElement);
                return;
            }

            final String fqcn = resourceFqcn.substring(0, resourceFqcn.length() - 8);
            final String scn = resourceScn.substring(0, resourceScn.length() - 8);
            final String xmlName = def(resource.getXmlName(), xmlify(scn));

            // --------------------------------------------------
            // Schema root element for root resource
            // --------------------------------------------------

            final Element elementElement = new Element("xs:element", XSD);
            elementElement.addAttribute(new Attribute("name", xmlName));

            final Element typeElement = new Element("xs:complexType", XSD);
            if (parentType == null) {
                elementElement.appendChild(typeElement);
                docInfo.rootElementDecls.put(xmlName, elementElement);
            } else {
                parentType.appendChild(elementElement);
                elementElement.addAttribute(new Attribute("type", xmlName));
                typeElement.addAttribute(new Attribute("name", xmlName));
                docInfo.typeDecls.put(xmlName, typeElement);
            }

            final Element typeSeqElement = new Element("xs:sequence", XSD);
            typeElement.appendChild(typeSeqElement);

            addDocumentation(elementElement, "RESOURCE DESCRIPTION");

            // --------------------------------------------------
            // Code model class instances
            // --------------------------------------------------

            final JDefinedClass parserClass;
            final JDefinedClass deparserClass;
            final JDefinedClass builderInterface;
            final JDefinedClass builderClass;
            final JDefinedClass builderFactoryClass;
            final JDefinedClass implementationClass;
            final JDefinedClass resourceNodeClass;
            final JDefinedClass coreClass;

            final JClass resourceInterface = (JClass) CodeModelUtils.typeFor(env, codeModel, resource.getResourceInterface());

            try {
                parserClass = codeModel._class(PUBLIC | FINAL, fqcn + "ParserImpl", CLASS);
                deparserClass = codeModel._class(PUBLIC | FINAL, fqcn + "DeparserImpl", CLASS);
                builderInterface = codeModel._class(PUBLIC, fqcn + "Builder", INTERFACE);
                builderClass = codeModel._class(PUBLIC | FINAL, fqcn + "BuilderImpl", CLASS);
                implementationClass = codeModel._class(PUBLIC | FINAL, fqcn + "ResourceImpl", CLASS);
                resourceNodeClass = codeModel._class(PUBLIC | FINAL, fqcn + "NodeImpl", CLASS);
                coreClass = parentType == null ? codeModel._class(PUBLIC | FINAL, fqcn, CLASS) : null;
                builderFactoryClass = codeModel._class(PUBLIC | FINAL, fqcn + "BuilderFactory", CLASS);
            } catch (JClassAlreadyExistsException e) {
                messager.printMessage(ERROR, "Duplicate class generation for " + fqcn);
                return;
            }

            // --------------------------------------------------
            // Class headers, extends/implements
            // --------------------------------------------------

            parserClass.annotate(Generated.class).paramArray("value").param(Generator.class.getName()).param(GENERATOR_VERSION);
            deparserClass.annotate(Generated.class).paramArray("value").param(Generator.class.getName()).param(GENERATOR_VERSION);
            builderInterface.annotate(Generated.class).paramArray("value").param(Generator.class.getName()).param(GENERATOR_VERSION);
            builderClass.annotate(Generated.class).paramArray("value").param(Generator.class.getName()).param(GENERATOR_VERSION);
            implementationClass.annotate(Generated.class).paramArray("value").param(Generator.class.getName()).param(GENERATOR_VERSION);
            resourceNodeClass.annotate(Generated.class).paramArray("value").param(Generator.class.getName()).param(GENERATOR_VERSION);
            if (parentType == null) coreClass.annotate(Generated.class).paramArray("value").param(Generator.class.getName()).param(GENERATOR_VERSION);

            final JTypeVar builderInterfaceP = builderInterface.generify("P");
            builderInterface._extends(codeModel.ref(NestedBuilder.class).erasure().erasure().narrow(builderInterfaceP));

            builderInterface.javadoc().add("A builder for " + resource.getName() + " resources.  This interface was automatically generated.");

            final JTypeVar builderClassP = builderClass.generify("P");
            builderClass._implements(builderInterface.narrow(builderClassP));

            parserClass._implements(codeModel.ref(ModelNodeParser.class).erasure().narrow(builderInterface.narrow(codeModel.wildcard())));
            deparserClass._implements(codeModel.ref(ModelNodeDeparser.class).erasure().narrow(resourceInterface));

            implementationClass._extends(codeModel.ref(AbstractResource.class));
            implementationClass._implements(resourceInterface);

            resourceNodeClass._extends(codeModel.ref(ResourceNode.class).erasure().narrow(resourceInterface));

            final JTypeVar builderFactoryP = builderFactoryClass.generify("P");
            builderFactoryClass._implements(codeModel.ref(BuilderFactory.class).erasure().narrow(builderFactoryP, builderInterface.erasure().narrow(builderFactoryP)));

            final JMethod implConstructor = implementationClass.constructor(PUBLIC);
            final JBlock implConstructorBody = implConstructor.body();
            final JInvocation implConstructorSuperCall = implConstructorBody.invoke("super");
            implConstructorSuperCall.arg(implConstructor.param(FINAL, codeModel.ref(String.class), "preComment"));
            implConstructorSuperCall.arg(implConstructor.param(FINAL, codeModel.ref(String.class), "postComment"));
            implConstructorSuperCall.arg(implConstructor.param(FINAL, codeModel.ref(String.class), "name"));
            implConstructorSuperCall.arg(implConstructor.param(FINAL, codeModel.ref(Resource.class), "parent"));

            // The core class is not instantiatable
            coreClass.constructor(PRIVATE);

            // --------------------------------------------------
            // Impl class stuff
            // --------------------------------------------------

            final JMethod implNavigateMethod = implementationClass.method(PUBLIC, Resource.class, "navigate");
            final JVar implNavigateMethodKey = implNavigateMethod.param(FINAL, String.class, "key");
            final JVar implNavigateMethodValue = implNavigateMethod.param(FINAL, String.class, "value");
            final JBlock implNavigateMethodBody = implNavigateMethod.body();
            implNavigateMethodBody._return(JExpr._null()); // todo

            // --------------------------------------------------
            // Builder stuff
            // --------------------------------------------------

            final JFieldVar builderClassParentField = builderClass.field(PRIVATE | FINAL, builderClassP, "parent");
            final JMethod builderClassConstructor = builderClass.constructor(NONE);
            builderClassConstructor.body().assign(JExpr._this().ref(builderClassParentField), builderClassConstructor.param(FINAL, builderClassP, "parent"));
            builderClass.method(PUBLIC, builderClassP, "end").body()._return(builderClassParentField);

            final JMethod builderFactoryConstructMethod = builderFactoryClass.method(PUBLIC, builderClass.narrow(builderFactoryP), "construct");
            builderFactoryConstructMethod.body()._return(JExpr._new(builderClass.narrow(builderFactoryP)).arg(builderFactoryConstructMethod.param(FINAL, builderFactoryP, "parent")));

            if (parentType == null) {
                final JMethod coreBuildMethod = coreClass.method(PUBLIC | STATIC, builderFactoryClass, "build");
                coreBuildMethod.javadoc().add("Introduce this resource type into a builder which supports nested polymorphic types.");
                coreBuildMethod.javadoc().add("Normally this method should not be directly invoked, but rather as part of a builder invocation chain.");
                coreBuildMethod.javadoc().addReturn().add("the appropriate builder factory");
                final JTypeVar coreBuildMethodP = coreBuildMethod.generify("P");
                coreBuildMethod.type(builderFactoryClass.narrow(coreBuildMethodP));
                coreBuildMethod.body()._return(JExpr._new(builderFactoryClass.narrow(coreBuildMethodP)));
            }

            // --------------------------------------------------
            // Resource parse/deparse methods
            // --------------------------------------------------

            final JMethod parseMethod = parserClass.method(PUBLIC, codeModel.VOID, "parse");
            parseMethod._throws(codeModel.ref(XMLStreamException.class));
            final JVar parseMethodStreamReader = parseMethod.param(codeModel.ref(XMLStreamReader.class), "streamReader");
            final JVar parseMethodBuilder = parseMethod.param(builderInterface.narrow(codeModel.wildcard()), "builder");
            final JBlock parseMethodBody = parseMethod.body();

            final JMethod deparseMethod = deparserClass.method(PUBLIC, codeModel.VOID, "deparse");
            deparseMethod._throws(codeModel.ref(XMLStreamException.class));
            final JVar deparseMethodStreamWriter = deparseMethod.param(codeModel.ref(XMLStreamWriter.class), "streamWriter");
            final JVar deparseMethodResource = deparseMethod.param(resourceInterface, "resource");
            final JBlock deparseMethodBody = deparseMethod.body();
            final JVar deparsePreComment = deparseMethodBody.decl(FINAL, codeModel.ref(String.class), "preComment", JExpr.invoke(deparseMethodResource, "getPreComment"));
            final JVar deparsePostComment = deparseMethodBody.decl(FINAL, codeModel.ref(String.class), "postComment", JExpr.invoke(deparseMethodResource, "getPostComment"));
            deparseMethodBody._if(deparsePreComment.ne(JExpr._null()))._then().invoke(deparseMethodStreamWriter, "writeComment").arg(deparsePreComment);
            deparseMethodBody.invoke(deparseMethodStreamWriter, "writeStartElement").arg(xmlNamespace).arg(xmlName);
            final JBlock deparseMethodAttributesBlock = deparseMethodBody.block();
            final JBlock deparseMethodElementsBlock = deparseMethodBody.block();
            deparseMethodBody._if(deparsePostComment.ne(JExpr._null()))._then().invoke(deparseMethodStreamWriter, "writeComment").arg(deparsePostComment);
            deparseMethodBody.invoke(deparseMethodStreamWriter, "writeEndElement");

            // --------------------------------------------------
            // Sub-resources
            // --------------------------------------------------

            for (SubResourceBuilderImpl<?> resourceBuilder : resource.getSubResources()) {
                new ResourceWriter(resourceBuilder, docInfo, xmlNamespace, typeElement).generate();
            }

            // --------------------------------------------------
            // Attributes
            // --------------------------------------------------

            class AttributeGenerator {
                final AttributeBuilderImpl<?> attributeBuilder;

                AttributeGenerator(final AttributeBuilderImpl<?> builder) {
                    attributeBuilder = builder;
                }

                void generate() {

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
                    final String attrVarName = fieldify(name);
                    final String getterName;
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
                        final Element elem = new Element("xs:element", XSD);
                        typeSeqElement.appendChild(elem);
                        elem.addAttribute(new Attribute("name", attrXmlName));
                        elem.addAttribute(new Attribute("type", "???")); // todo XML type
                        elem.addAttribute(new Attribute("minOccurs", required ? "1" : "0"));
                        elem.addAttribute(new Attribute("maxOccurs", "1"));
                        addDocumentation(elem, attributeBuilder.getRootDescription());
                    } else {
                        assert as == XmlRender.As.ATTRIBUTE;
                        final Element attr = new Element("xs:attribute", XSD);
                        elementElement.appendChild(attr);
                        attr.addAttribute(new Attribute("name", attrXmlName));
                        attr.addAttribute(new Attribute("type", "???"));// todo XML type
                        attr.addAttribute(new Attribute("use", required ? "required" : "optional"));
                        addDocumentation(attr, attributeBuilder.getRootDescription());
                    }

                    // --------------------------------------------------
                    // Parser / Deparser
                    // --------------------------------------------------

                    if (codeModel.ref(VirtualAttribute.class).erasure() == null) {
                        // ----------------------------------------------
                        // Deparser
                        // ----------------------------------------------
                        if (as == XmlRender.As.ELEMENT) {
                            // real attributes get persisted
                            deparseMethodElementsBlock.invoke(deparseMethodStreamWriter, "writeStartElement").arg(JExpr.lit(xmlNamespace)).arg(JExpr.lit(xmlName));
                            // todo emit attribute content
                            deparseMethodElementsBlock.invoke(deparseMethodStreamWriter, "writeEndElement");
                        } else {
                            deparseMethodAttributesBlock.invoke(deparseMethodStreamWriter, "writeAttribute").arg(JExpr.lit(xmlName)).arg("...value...");
                        }

                        // ----------------------------------------------
                        // Parser
                        // ----------------------------------------------

                    }

                    // --------------------------------------------------
                    // Generate impl class field, getter, & constructor param
                    // --------------------------------------------------

                    final JMethod getterMethod = implementationClass.method(PUBLIC, attributeJType, getterName);
                    final JBlock getterMethodBody = getterMethod.body();
                    if (access.isReadable()) {
                        if (virtual != null) {
                            // todo - cache virtual instances
                            getterMethodBody._return(JExpr.invoke(JExpr._new(codeModel.ref(VirtualAttribute.class).erasure().narrow(attributeJType)), "getValue"));
                        } else {
                            final JFieldVar field = implementationClass.field(PRIVATE | FINAL, attributeJType, attrVarName);
                            implConstructorBody.assign(JExpr._this().ref(field), implConstructor.param(FINAL, attributeJType, attrVarName));
                            getterMethodBody._return(field);
                        }
                    } else {
                        getterMethodBody._throw(codeModel.ref(AbstractResource.class).staticInvoke("notReadable"));
                    }

                    // --------------------------------------------------
                    // Generate builder field and setter method
                    // --------------------------------------------------

                    if (access.isWritable()) {
                        final JMethod builderInterfaceSetMethod;
                        final JMethod builderClassSetMethod;

                        // todo - better classification needed
                        if (attributeJType.erasure().fullName().equals("java.util.Map")) {
                            final JClass attributeJClass = (JClass) attributeJType;
                            final JClass keyType;
                            final JClass valueType;
                            if (attributeJClass.getTypeParameters().size() != 2) {
                                keyType = valueType = codeModel.ref(Object.class);
                            } else {
                                keyType = attributeJClass.getTypeParameters().get(0);
                                valueType = attributeJClass.getTypeParameters().get(1);
                            }

                            if (true /* todo detect value type */) {

                                // value is a simple type

                                final JClass listType = codeModel.ref(ArrayList.class).narrow(codeModel.ref(Entry.class).narrow(keyType, valueType));
                                final JFieldVar attributeField = builderClass.field(PRIVATE | FINAL, listType, attrVarName);
                                attributeField.init(JExpr._new(listType));

                                builderInterfaceSetMethod = builderInterface.method(NONE, builderInterface.narrow(builderInterfaceP), "add" + name);
                                builderInterfaceSetMethod.param(keyType, "key");
                                builderInterfaceSetMethod.param(valueType, "value");
                                builderClassSetMethod = builderClass.method(NONE, builderInterface.narrow(builderInterfaceP), "add" + name);
                                final JVar keyParam = builderClassSetMethod.param(FINAL, keyType, "key");
                                final JVar valueParam = builderClassSetMethod.param(FINAL, valueType, "value");
                                final JBlock body = builderClassSetMethod.body();
                                attributeField.invoke("add").arg(JExpr._new(codeModel.ref(Entry.class).narrow(keyType, valueType)).arg(keyParam).arg(valueParam));
                                body._return(JExpr._this());
                            } else {
                                // value is a complex (resource) type

                                final String nestedBuilderTypeName = valueType.fullName() + "BuilderImpl";
                                final JClass nestedBuilderType = codeModel._getClass(nestedBuilderTypeName);
                                if (nestedBuilderType == null) {
                                    messager.printMessage(ERROR, "No builder was generated for non-simple attribute type " + nestedBuilderTypeName);
                                    return;
                                }

                                final JClass listType = codeModel.ref(ArrayList.class).narrow(codeModel.ref(Entry.class).narrow(keyType, nestedBuilderType));
                                final JFieldVar attributeField = builderClass.field(PRIVATE | FINAL, listType, attrVarName);
                                attributeField.init(JExpr._new(listType));

                                final JClass narrowedBuilderType = nestedBuilderType.narrow(builderInterface.narrow(builderInterfaceP));
                                builderInterfaceSetMethod = builderInterface.method(NONE, narrowedBuilderType, "add" + name);
                                builderInterfaceSetMethod.param(keyType, "key");

                                builderClassSetMethod = builderClass.method(NONE, narrowedBuilderType, "add" + name);
                                final JVar keyParam = builderClassSetMethod.param(FINAL, keyType, "key");
                                final JBlock body = builderClassSetMethod.body();
                                final JVar valueVar = body.decl(nestedBuilderType, "_builder", JExpr._new(narrowedBuilderType).arg(JExpr._this()));
                                attributeField.invoke("add").arg(JExpr._new(codeModel.ref(Entry.class).narrow(keyType, valueType)).arg(keyParam).arg(valueVar));
                                body._return(valueVar);
                            }
                        } else {
                            // value is a simple type...
                            // todo collections, sub-resources, attribute groups all need special builders

                            final JFieldVar attributeField = builderClass.field(PRIVATE, attributeJType, attrVarName);
                            if (defaultValueExpr != null) {
                                attributeField.assign(defaultValueExpr);
                            }

                            builderInterfaceSetMethod = builderInterface.method(NONE, builderInterface.narrow(builderInterfaceP), attrVarName);
                            builderClassSetMethod = builderClass.method(PUBLIC, builderInterface.narrow(builderInterfaceP), attrVarName);

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
                            javadoc.addParam(attrVarName).append("the " + attributeBuilder.getRootDescription());
                            javadoc.addReturn().append("this builder");

                            // todo for complex types, we must deconstruct the type and create a method param for each component...
                            builderInterfaceSetMethod.param(attributeJType, attrVarName);
                            final JVar newValueParam = builderClassSetMethod.param(attributeJType, attrVarName);

                            final JBlock body = builderClassSetMethod.body();
                            if (! validators.isEmpty()) {

                                // --------------------------------------------------
                                // Emit validation block
                                // --------------------------------------------------

                                final JVar context = body.decl(FINAL, codeModel.ref(ExceptionThrowingValidationContext.class), "_context", JExpr._new(codeModel.ref(ExceptionThrowingValidationContext.class)));
                                for (DeclaredType validator : validators) {
                                    final JClass validatorType = (JClass) CodeModelUtils.typeFor(env, codeModel, validator);
                                    // todo - cache validator instances
                                    final JInvocation inv = body.invoke(JExpr._new(validatorType.erasure().narrow(resourceInterface, attributeJType.boxify())), "validate");
                                    inv.arg(JExpr._null()); // TODO - no resource yet...?
                                    inv.arg(JExpr.lit(attrVarName));
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
                }
            }
            for (AttributeBuilderImpl<?> attributeBuilder : resource.getAttributes()) {
                new AttributeGenerator(attributeBuilder).generate();
            }
        }
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
                } else if (builder.length() > 0 && Character.isUpperCase(c) && Character.isLowerCase(n) || wordDone) {
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

    /*
      BLAFunnyJavaKindAThing -> blaFunnyJavaKindAThing
      FOO -> foo
      Foo -> foo
      foo -> foo
      FOOBar -> fooBar
     */
    private static String fieldify(String camelHumpsName) {
        final int length = camelHumpsName.length();
        final StringBuilder builder = new StringBuilder(length);
        int idx = 0;
        int c = camelHumpsName.codePointAt(idx), n;
        if (Character.isLowerCase(c)) {
            return camelHumpsName;
        }
        builder.appendCodePoint(Character.toLowerCase(c));
        idx = camelHumpsName.offsetByCodePoints(idx, 1);
        c = camelHumpsName.codePointAt(idx);
        for (;;) {
            idx = camelHumpsName.offsetByCodePoints(idx, 1);
            if (idx < length) {
                n = camelHumpsName.codePointAt(idx);
                if (Character.isLowerCase(n)) {
                    // next is lowercase; we're done
                    builder.appendCodePoint(c);
                    builder.append(camelHumpsName.substring(idx));
                    return builder.toString();
                } else {
                    builder.appendCodePoint(Character.toLowerCase(c));
                    c = n;
                }
            } else {
                builder.appendCodePoint(Character.toLowerCase(c));
                return builder.toString();
            }
        }
    }

    private static String buildNamespace(final RootResource.Kind kind, final String namespace, final String version) {
        StringBuilder b = new StringBuilder(64);
        if (kind == RootResource.Kind.SYSTEM) {
            b.append("sys:");
        } else {
            b.append("ext:");
        }
        b.append(namespace).append(':').append(version);
        return b.toString();
    }

    private static void addDocumentation(final Element element, final String documentation) {
        final Element annotation = new Element("xs:annotation", XSD);
        element.appendChild(annotation);
        final Element documentationElement = new Element("xs:documentation", XSD);
        annotation.appendChild(documentationElement);
        documentationElement.appendChild(documentation);
    }

}
