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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import org.jboss.mgmt.AttributeValidator;
import org.jboss.mgmt.Resource;
import org.jboss.mgmt.annotation.Access;
import org.jboss.mgmt.annotation.Attribute;
import org.jboss.mgmt.annotation.AttributeGroup;
import org.jboss.mgmt.annotation.AttributeType;
import org.jboss.mgmt.annotation.Enumerated;
import org.jboss.mgmt.annotation.Provides;
import org.jboss.mgmt.annotation.Reference;
import org.jboss.mgmt.annotation.Required;
import org.jboss.mgmt.annotation.RootResource;
import org.jboss.mgmt.annotation.Schema;
import org.jboss.mgmt.annotation.xml.XmlName;
import org.jboss.mgmt.annotation.xml.XmlRender;
import org.jboss.mgmt.annotation.xml.XmlTypeName;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.WARNING;
import static org.jboss.mgmt.generator.AnnotationUtils.annotationIs;
import static org.jboss.mgmt.generator.AnnotationUtils.booleanValue;
import static org.jboss.mgmt.generator.AnnotationUtils.classArrayValue;
import static org.jboss.mgmt.generator.AnnotationUtils.classValue;
import static org.jboss.mgmt.generator.AnnotationUtils.getAnnotationValue;
import static org.jboss.mgmt.generator.AnnotationUtils.getAnnotationValueEnumConst;
import static org.jboss.mgmt.generator.AnnotationUtils.getAnnotationValueString;
import static org.jboss.mgmt.generator.AnnotationUtils.stringArrayValue;
import static org.jboss.mgmt.generator.GeneratorUtils.buildNamespace;
import static org.jboss.mgmt.generator.GeneratorUtils.constify;
import static org.jboss.mgmt.generator.GeneratorUtils.def;
import static org.jboss.mgmt.generator.GeneratorUtils.fieldify;
import static org.jboss.mgmt.generator.GeneratorUtils.singular;
import static org.jboss.mgmt.generator.GeneratorUtils.without;
import static org.jboss.mgmt.generator.GeneratorUtils.xmlify;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ProcessingContext {
    private final ProcessingEnvironment env;
    private final RoundEnvironment roundEnv;

    private final Map<TypeElement, AttributeTypeInfo> attributeTypeInfoMap = new IdentityHashMap<TypeElement, AttributeTypeInfo>();
    private final Map<TypeElement, AttributeGroupInfo> attributeGroupInfoMap = new IdentityHashMap<TypeElement, AttributeGroupInfo>();
    private final Map<TypeElement, NewSchemaInfo> schemaInfoMap = new IdentityHashMap<TypeElement, NewSchemaInfo>();
    private final Map<TypeElement, RootResourceInfo> rootResourceInfoMap = new IdentityHashMap<TypeElement, RootResourceInfo>();

    private final Set<TypeElement> inFlightAttributeTypes = Collections.newSetFromMap(new IdentityHashMap<TypeElement, Boolean>());
    private final Set<TypeElement> inFlightAttributeGroups = Collections.newSetFromMap(new IdentityHashMap<TypeElement, Boolean>());

    ProcessingContext(final ProcessingEnvironment env, final RoundEnvironment roundEnv) {
        this.env = env;
        this.roundEnv = roundEnv;
    }

    public NewSchemaInfo processSchema(final TypeElement schemaElement) {
        if (schemaInfoMap.containsKey(schemaElement)) {
            return schemaInfoMap.get(schemaElement); // may be {@code null}
        }
        final NewSchemaInfo info;
        schemaInfoMap.put(schemaElement, info = processNewSchema(schemaElement));
        return info;
    }

    private NewSchemaInfo processNewSchema(final TypeElement schemaAnnotation) {
        final Messager messager = env.getMessager();

        if (schemaAnnotation.getKind() != ElementKind.ANNOTATION_TYPE) {
            messager.printMessage(ERROR, "@Schema may only be applied to an annotation type");
            return null;
        }

        final Schema schema = schemaAnnotation.getAnnotation(Schema.class);
        final String[] namespaces = schema.compatibilityNamespaces();
        final Schema.Kind kind = schema.kind();
        final String namespace = schema.namespace();
        final String schemaLocation = schema.schemaLocation();
        final String schemaVersion = schema.version();
        final String xmlNamespace = buildNamespace(kind, namespace, schemaVersion);
        final URI schemaLocationUri;
        try {
            schemaLocationUri = new URI(schemaLocation);
        } catch (URISyntaxException e) {
            messager.printMessage(ERROR, "Namespace schema location '" + schemaLocation + "' is not valid for " + xmlNamespace);
            return null;
        }
        final String schemaLocationPath = schemaLocationUri.getPath();
        if (schemaLocationPath == null) {
            messager.printMessage(ERROR, "Namespace schema location '" + schemaLocation + "' does not have a path component for " + xmlNamespace);
            return null;
        }
        final String schemaLocationFileName = schemaLocationPath.substring(schemaLocationPath.lastIndexOf('/') + 1);
        if (! schemaLocationFileName.endsWith(".xsd")) {
            messager.printMessage(WARNING, "Namespace schema location '" + schemaLocation + "' should specify a file name ending in \".xsd\"");
        }
        final ArrayList<RootResourceInfo> resourceList = new ArrayList<RootResourceInfo>();

        for (Element element : roundEnv.getElementsAnnotatedWith(schemaAnnotation)) {
            if (element.getKind() != ElementKind.INTERFACE && ! (element instanceof TypeElement)) {
                messager.printMessage(ERROR, "Annotation for schema (" + schemaAnnotation + ") may only be applied to interfaces");
                continue;
            }
            final TypeElement typeElement = (TypeElement) element;
            if (element.getAnnotation(RootResource.class) != null) {
                RootResourceInfo info = processRootResource(typeElement);
                if (info != null) resourceList.add(info);
            } else {
                messager.printMessage(WARNING, "Ignoring " + schemaAnnotation + " annotation on interface " + typeElement.getQualifiedName(), typeElement);
            }
        }

        final RootResourceInfo[] rootResources = resourceList.toArray(new RootResourceInfo[resourceList.size()]);
        return new NewSchemaInfo(schemaVersion, namespace, kind, schemaLocation, schemaLocationFileName, namespaces, rootResources);
    }

    public RootResourceInfo processRootResource(TypeElement resourceInterface) {
        if (rootResourceInfoMap.containsKey(resourceInterface)) {
            return rootResourceInfoMap.get(resourceInterface); // may be {@code null}
        }
        RootResourceInfo info;
        rootResourceInfoMap.put(resourceInterface, info = processNewRootResource(resourceInterface));
        return info;
    }

    private RootResourceInfo processNewRootResource(final TypeElement resourceInterface) {
        RootResource rootResource = resourceInterface.getAnnotation(RootResource.class);
        final String simpleName = resourceInterface.getSimpleName().toString();
        final String name = rootResource.name().isEmpty() ? xmlify(simpleName) : rootResource.name();
        final String type = rootResource.type();
        final Provides provides = resourceInterface.getAnnotation(Provides.class);
        final XmlName xmlName = resourceInterface.getAnnotation(XmlName.class);
        final ResourceInfo resourceInfo = processResource(resourceInterface);
        if (resourceInfo == null) {
            return null;
        }
        return new RootResourceInfo(name, type, provides == null ? new String[0] : provides.value(), xmlName == null ? name : xmlName.value(), resourceInfo);
    }

    public ResourceInfo processResource(final TypeElement resourceInterface) {
        final List<ResourceMember> members = new ArrayList<ResourceMember>();
        for (Element element : resourceInterface.getEnclosedElements()) {
            if (element.getKind() == ElementKind.METHOD) {
                ExecutableElement executableElement = (ExecutableElement) element;
                if (element.getAnnotation(Attribute.class) != null) {
                    final AttributeInfo attributeInfo = processAttribute(executableElement);
                    if (attributeInfo != null) {
                        members.add(attributeInfo);
                    }
                } else if (element.getAnnotation(AttributeGroup.class) != null) {
                    final AttributeGroupInfo groupInfo = processAttributeGroup(executableElement.getReturnType());
                    if (groupInfo != null) {
                        members.add(groupInfo);
                    }
                } else {
                    env.getMessager().printMessage(ERROR, "Unknown member on resource", executableElement);
                }
            }
        }
        return new ResourceInfo(resourceInterface, members.toArray(new ResourceMember[members.size()]));
    }

    public AttributeValueInfo processAttributeValue(String name, ExecutableElement declaringElement) {
        final Messager messager = getEnv().getMessager();
        final TypeMirror type = declaringElement.getReturnType();

        if (type.getKind() == TypeKind.VOID || type.getKind() == TypeKind.NONE) {
            messager.printMessage(ERROR, "Attribute methods must have a non-void return value", declaringElement);
            return null;
        }

        String defaultVarName = constify(name) + "_DEFAULT";
        VariableElement defaultVal = null;

        String[] enumerations = null;
        boolean required = true;

        DeclaredType referenceType = null;
        boolean monitor = false;

        for (AnnotationMirror annotationMirror : declaringElement.getAnnotationMirrors()) {
            if (annotationIs(annotationMirror, Enumerated.class)) {
                enumerations = stringArrayValue(getAnnotationValue(annotationMirror, "name"));
            } else if (annotationIs(annotationMirror, Required.class)) {
                required = booleanValue(getAnnotationValue(annotationMirror, "value"), true);
            } else if (annotationIs(annotationMirror, Reference.class)) {
                referenceType = (DeclaredType) classValue(getAnnotationValue(annotationMirror, "referenceType"));
                monitor = booleanValue(getAnnotationValue(annotationMirror, "monitor"), false);
            }
        }

        for (VariableElement fieldElement : ElementFilter.fieldsIn(declaringElement.getEnclosingElement().getEnclosedElements())) {
            if (defaultVarName.equals(fieldElement.getSimpleName().toString())) {
                if (! isAssignable(fieldElement.asType(), type)) {
                    messager.printMessage(ERROR, "Cannot assign default property value to property of type " + type, fieldElement);
                } else {
                    defaultVal = fieldElement;
                }
                break;
            }
        }

        if (isAssignable(type, String.class)) {
            if (referenceType != null) {
                if (enumerations != null) {
                    messager.printMessage(WARNING, "Enumerations will not be used for attribute of type " + type, declaringElement);
                }
                if (defaultVal != null) {
                    messager.printMessage(WARNING, "Default value will not be used for attribute group of type " + type, defaultVal);
                }
                return new ReferenceAttributeValueInfo(name, required, referenceType, monitor);
            }
            return new StringAttributeValueInfo(name, defaultVal, required, enumerations);
        }

        if (referenceType != null) {
            messager.printMessage(ERROR, "Reference types must have a string value", declaringElement);
            return null;
        }

        if (enumerations != null) {
            messager.printMessage(WARNING, "Enumerations will not be used for attribute of type " + type, declaringElement);
        }

        if (type.getKind().isPrimitive()) {
            return new PrimitiveAttributeValueInfo(type.getKind(), name, defaultVal, required);
        }

        if (defaultVal != null) {
            messager.printMessage(WARNING, "Default value will not be used for attribute group of type " + type, defaultVal);
        }

        if (declaringElement.getAnnotation(AttributeGroup.class) != null) {
            final AttributeGroupInfo attributeGroupInfo = processAttributeGroup(type);
            if (attributeGroupInfo == null) {
                return null;
            }
            return new AttributeGroupValueInfo(name, attributeGroupInfo, required);
        }

        // have to check the return type for @AttributeType
        if (type instanceof DeclaredType && ((DeclaredType)type).asElement().getAnnotation(AttributeType.class) != null) {
            final AttributeTypeInfo attributeTypeInfo = processAttributeType(type);
            if (attributeTypeInfo == null) {
                return null;
            }
            return new AttributeTypeValueInfo(name, attributeTypeInfo, required);
        }

        // only polys left...

        // todo - allow overrides in the event that our algorithm fails horribly
        final String singular = singular(name);

        if (isSameType(Map.class, type) || isSameType(SortedMap.class, type) || isSameType(NavigableMap.class, type)) {
            final DeclaredType keyType;
            final DeclaredType valueType;
            // map type
            final List<? extends TypeMirror> typeArguments = ((DeclaredType) type).getTypeArguments();
            if (typeArguments.size() != 2) {
                messager.printMessage(ERROR, "Map attributes must have two type arguments", declaringElement);
                keyType = (DeclaredType) getType(String.class);
                valueType = (DeclaredType) getType(String.class);
            } else {
                if (!(typeArguments.get(0) instanceof DeclaredType)) {
                    messager.printMessage(ERROR, "Map key type must be a concrete type", declaringElement);
                    keyType = (DeclaredType) getType(String.class);
                } else {
                    keyType = (DeclaredType) typeArguments.get(0);
                }
                if (!(typeArguments.get(1) instanceof DeclaredType)) {
                    messager.printMessage(ERROR, "Map value type must be a concrete type", declaringElement);
                    valueType = (DeclaredType) getType(String.class);
                } else {
                    valueType = (DeclaredType) typeArguments.get(1);
                }
            }
            return new MapAttributeValueInfo(name, singular, keyType, valueType, required, isAssignable(type, SortedMap.class));
        }

        TypeMirror valueType;

        if (type.getKind() == TypeKind.ARRAY) {
            valueType = ((ArrayType) type).getComponentType();
            return new ArrayAttributeValueInfo(name, singular, valueType, required);
        } else if (isSameType(List.class, type) || isSameType(Set.class, type) || isSameType(SortedSet.class, type) || isSameType(NavigableSet.class, type)) {
            final List<? extends TypeMirror> typeArguments = ((DeclaredType) type).getTypeArguments();
            if (typeArguments.size() != 1) {
                messager.printMessage(ERROR, "Collection attributes must have one type argument", declaringElement);
                valueType = getType(String.class);
            } else {
                valueType = typeArguments.get(0);
            }
            return new CollectionAttributeValueInfo(name, singular, (DeclaredType) valueType, required, (DeclaredType) type);
        } else {
            messager.printMessage(ERROR, "Unknown or disallowed attribute type", declaringElement);
            return null;
        }
    }

    public AttributeGroupInfo processAttributeGroup(TypeMirror typeMirror) {
        final TypeElement element = (TypeElement) ((DeclaredType) typeMirror).asElement();
        if (attributeGroupInfoMap.containsKey(element)) {
            return attributeGroupInfoMap.get(element); // may be {@code null}
        }
        AttributeGroupInfo info;
        attributeGroupInfoMap.put(element, info = processNewAttributeGroup(element));
        return info;
    }

    private AttributeGroupInfo processNewAttributeGroup(final TypeElement typeElement) {
        final ProcessingEnvironment env = getEnv();
        final Messager messager = env.getMessager();
        if (! startAttributeGroup(typeElement)) {
            messager.printMessage(ERROR, "Attribute group is cyclic", typeElement);
            return null;
        }
        try {
            if (typeElement.getKind() != ElementKind.INTERFACE) {
                messager.printMessage(ERROR, "Attribute group must be an interface", typeElement);
                return null;
            }
            if (typeElement.getSimpleName().toString().endsWith("Resource") || isAssignable(typeElement.asType(), Resource.class)) {
                messager.printMessage(ERROR, "Attribute group must not be a resource");
                return null;
            }

            final ArrayList<AttributeInfo> attributeInfoList = new ArrayList<AttributeInfo>();

            // all methods of an attribute type are attributes; they do not need @Attribute
            for (ExecutableElement executableElement : ElementFilter.methodsIn(env.getElementUtils().getAllMembers(typeElement))) {
                final AttributeInfo info = processAttribute(executableElement);
                if (! info.getValueInfo().isValidInAttributeType()) {
                    messager.printMessage(ERROR, "Attribute with this type cannot be used in an AttributeType", executableElement);
                } else if (info != null) attributeInfoList.add(info);
            }

            final AttributeGroupInfo info = new AttributeGroupInfo(typeElement, attributeInfoList.toArray(new AttributeInfo[attributeInfoList.size()]));
            addAttributeGroupInfo(info);
            return info;
        } finally {
            finishAttributeGroup(typeElement);
        }
    }

    public AttributeInfo processAttribute(ExecutableElement executableElement) {
        final String getterName = executableElement.getSimpleName().toString();

        String name;
        if (getterName.startsWith("get")) {
            name = fieldify(getterName.substring(3));
        } else if (getterName.startsWith("is")) {
            name = fieldify(getterName.substring(2));
        } else {
            name = getterName;
        }

        Access access = null;
        TypeMirror virtual = null;
        boolean required = true;
        VariableElement defaultVal = null;
        TypeMirror[] validators = null;
        String xmlName = xmlify(name);

        for (AnnotationMirror annotationMirror : executableElement.getAnnotationMirrors()) {
            if (annotationIs(annotationMirror, Attribute.class)) {
                name = def(getAnnotationValueString(annotationMirror, "name"), name);
                access = def(getAnnotationValueEnumConst(annotationMirror, "access", Access.class), Access.READ_WRITE);
                virtual = classValue(getAnnotationValue(annotationMirror, "virtual"));
                validators = classArrayValue(getAnnotationValue(annotationMirror, "validators"));
            } else if (annotationIs(annotationMirror, XmlName.class)) {
                xmlName = getAnnotationValueString(annotationMirror, "value");
            } else if (annotationIs(annotationMirror, Required.class)) {
                required = booleanValue(getAnnotationValue(annotationMirror, "value"), true);
            }
        }

        final AttributeValueInfo valueInfo = processAttributeValue(name, executableElement);
        if (valueInfo == null) {
            return null;
        }
        return new AttributeInfo(executableElement, name, valueInfo);
    }

    public AttributeTypeInfo processAttributeType(final TypeMirror type) {
        final TypeElement element = (TypeElement) ((DeclaredType) type).asElement();
        if (attributeTypeInfoMap.containsKey(element)) {
            return attributeTypeInfoMap.get(element); // may be {@code null}
        }
        AttributeTypeInfo info;
        attributeTypeInfoMap.put(element, info = processNewAttributeType(element));
        return info;
    }

    private AttributeTypeInfo processNewAttributeType(TypeElement typeElement) {
        final ProcessingEnvironment env = getEnv();
        final Messager messager = env.getMessager();
        if (! startAttributeType(typeElement)) {
            messager.printMessage(ERROR, "Attribute type is cyclic", typeElement);
            return null;
        }
        try {
            if (typeElement.getKind() != ElementKind.INTERFACE) {
                messager.printMessage(ERROR, "Attribute type must be an interface", typeElement);
                return null;
            }
            if (typeElement.getSimpleName().toString().endsWith("Resource") || isAssignable(typeElement.asType(), Resource.class)) {
                messager.printMessage(ERROR, "Attribute type must not be a resource");
                return null;
            }

            final Map<String, AnnotationMirror> mirrorMap = AnnotationUtils.mirrorListToMap(env.getElementUtils().getAllAnnotationMirrors(typeElement));

            final String interfaceName = typeElement.getQualifiedName().toString();
            final String interfaceSimpleName = typeElement.getSimpleName().toString();
            final String implClassName = interfaceName + "Impl";

            final AnnotationMirror attributeTypeMirror = mirrorMap.remove(AttributeType.class.getName());
            final AnnotationMirror xmlTypeNameMirror = mirrorMap.remove(XmlTypeName.class.getName());
            final AnnotationMirror xmlRenderMirror = mirrorMap.remove(XmlRender.class.getName());

            final String typeName = without(interfaceSimpleName, "Type");
            final String xmlTypeName = def(getAnnotationValueString(xmlTypeNameMirror, "value"), xmlify(typeName));
            final XmlRender.As renderAs = def(getAnnotationValueEnumConst(xmlRenderMirror, "as", XmlRender.As.class), XmlRender.As.ELEMENT);
            final boolean wrapperElement = booleanValue(getAnnotationValue(xmlRenderMirror, "wrapperElement"), true);
            final ArrayList<DeclaredType> validatorsList = new ArrayList<DeclaredType>();

            for (TypeMirror mirror : classArrayValue(getAnnotationValue(attributeTypeMirror, "validators"))) {
                if (mirror instanceof DeclaredType) {
                    final DeclaredType validatorType = (DeclaredType) mirror;
                    if (env.getTypeUtils().isAssignable(validatorType, env.getElementUtils().getTypeElement(AttributeValidator.class.getName()).asType())) {
                        validatorsList.add(validatorType);
                    } else {
                        messager.printMessage(ERROR, "Validator types must implement " + AttributeValidator.class, typeElement, attributeTypeMirror);
                    }
                } else {
                    messager.printMessage(ERROR, "Validator types must be non-primitive", typeElement, attributeTypeMirror);
                }
            }
            final DeclaredType[] validators = validatorsList.toArray(new DeclaredType[validatorsList.size()]);

            // now, process each attribute type member...

            final ArrayList<AttributeInfo> attributeInfoList = new ArrayList<AttributeInfo>();

            // all methods of an attribute type are attributes; they do not need @Attribute
            for (ExecutableElement executableElement : ElementFilter.methodsIn(env.getElementUtils().getAllMembers(typeElement))) {
                final AttributeInfo info = processAttribute(executableElement);
                if (! info.getValueInfo().isValidInAttributeType()) {
                    messager.printMessage(ERROR, "Attribute with this type cannot be used in an AttributeType", executableElement);
                } else if (info != null) attributeInfoList.add(info);
            }

            final AttributeTypeInfo info = new AttributeTypeInfo(typeElement, implClassName, xmlTypeName, renderAs, wrapperElement, validators, attributeInfoList.toArray(new AttributeInfo[attributeInfoList.size()]));
            addAttributeTypeInfo(info);
            return info;
        } finally {
            finishAttributeType(typeElement);
        }
    }

    boolean isSameType(final Class<?> type1, final TypeMirror type2) {
        return env.getTypeUtils().isSameType(getType(type1), type2);
    }

    boolean isAssignable(final TypeMirror source, final TypeMirror target) {
        return env.getTypeUtils().isAssignable(source, target);
    }

    boolean isAssignable(final Class<?> source, final TypeMirror target) {
        return isAssignable(getType(source), target);
    }

    boolean isAssignable(final TypeMirror source, final Class<?> target) {
        return isAssignable(source, getType(target));
    }

    public ProcessingEnvironment getEnv() {
        return env;
    }

    public RoundEnvironment getRoundEnv() {
        return roundEnv;
    }

    private boolean startAttributeGroup(final TypeElement element) {
        return inFlightAttributeGroups.add(element);
    }

    private void finishAttributeGroup(TypeElement element) {
        inFlightAttributeGroups.remove(element);
    }

    private boolean startAttributeType(TypeElement element) {
        return inFlightAttributeTypes.add(element);
    }

    private void finishAttributeType(TypeElement element) {
        inFlightAttributeTypes.remove(element);
    }

    private void addAttributeTypeInfo(final AttributeTypeInfo attributeTypeInfo) {
        attributeTypeInfoMap.put(attributeTypeInfo.getTypeElement(), attributeTypeInfo);
    }

    private void addAttributeGroupInfo(final AttributeGroupInfo attributeGroupInfo) {
        attributeGroupInfoMap.put(attributeGroupInfo.getTypeElement(), attributeGroupInfo);
    }

    TypeMirror getType(final Class<?> clazz) {
        return env.getElementUtils().getTypeElement(clazz.getName()).asType();
    }
}
