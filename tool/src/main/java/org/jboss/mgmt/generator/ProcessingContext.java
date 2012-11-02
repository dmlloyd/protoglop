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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.jboss.mgmt.annotation.SubResource;
import org.jboss.mgmt.annotation.XmlName;
import org.jboss.mgmt.annotation.XmlRender;
import org.jboss.mgmt.annotation.XmlTypeName;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
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
import javax.lang.model.util.Types;

import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.WARNING;
import static org.jboss.mgmt.generator.AnnotationUtils.annotationIs;
import static org.jboss.mgmt.generator.AnnotationUtils.booleanValue;
import static org.jboss.mgmt.generator.AnnotationUtils.classArrayValue;
import static org.jboss.mgmt.generator.AnnotationUtils.classValue;
import static org.jboss.mgmt.generator.AnnotationUtils.enumConstValue;
import static org.jboss.mgmt.generator.AnnotationUtils.getAnnotation;
import static org.jboss.mgmt.generator.AnnotationUtils.getAnnotationValue;
import static org.jboss.mgmt.generator.AnnotationUtils.getAnnotationValueEnumConst;
import static org.jboss.mgmt.generator.AnnotationUtils.getAnnotationValueString;
import static org.jboss.mgmt.generator.AnnotationUtils.stringArrayValue;
import static org.jboss.mgmt.generator.AnnotationUtils.stringValue;
import static org.jboss.mgmt.generator.NameUtils.buildNamespace;
import static org.jboss.mgmt.generator.NameUtils.constify;
import static org.jboss.mgmt.generator.GeneratorUtils.def;
import static org.jboss.mgmt.generator.NameUtils.fieldify;
import static org.jboss.mgmt.generator.NameUtils.namespaceify;
import static org.jboss.mgmt.generator.NameUtils.singular;
import static org.jboss.mgmt.generator.NameUtils.without;
import static org.jboss.mgmt.generator.NameUtils.xmlify;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ProcessingContext {

    private static final String[] NO_STRINGS = new String[0];

    private final ProcessingEnvironment env;
    private final RoundEnvironment roundEnv;

    private final Map<TypeElement, AttributeTypeInfo> attributeTypeInfoMap = new IdentityHashMap<TypeElement, AttributeTypeInfo>();
    private final Map<ExecutableElement, AttributeValueInfo> attributeValueInfoMap = new IdentityHashMap<ExecutableElement, AttributeValueInfo>();
    private final Map<TypeElement, AttributeGroupInfo> attributeGroupInfoMap = new IdentityHashMap<TypeElement, AttributeGroupInfo>();
    private final Map<TypeElement, NewSchemaInfo> schemaInfoMap = new IdentityHashMap<TypeElement, NewSchemaInfo>();
    private final Map<TypeElement, RootResourceInfo> rootResourceInfoMap = new IdentityHashMap<TypeElement, RootResourceInfo>();
    private final Map<ExecutableElement, SubResourceInfo> subResourceInfoMap = new IdentityHashMap<ExecutableElement, SubResourceInfo>();
    private final Set<ExecutableElement> affiliatedMethods = Collections.newSetFromMap(new IdentityHashMap<ExecutableElement, Boolean>());
    private final Map<TypeElement, ResourceInfo> resourceInfoMap = new IdentityHashMap<TypeElement, ResourceInfo>();

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

    private static final Pattern NAME_WITH_VERSION = Pattern.compile("^([a-zA-Z_][a-zA-Z0-9_]+)_(\\d+(?:_\\d+)*)$");

    private NewSchemaInfo processNewSchema(final TypeElement schemaAnnotation) {
        final Messager messager = env.getMessager();

        if (schemaAnnotation.getKind() != ElementKind.ANNOTATION_TYPE) {
            messager.printMessage(ERROR, "@Schema may only be applied to an annotation type");
            return null;
        }
        final String simpleName = schemaAnnotation.getSimpleName().toString();
        final Matcher matcher = NAME_WITH_VERSION.matcher(simpleName);
        final String baseName;
        final String baseVersion;
        if (matcher.matches()) {
            baseName = matcher.group(1);
            baseVersion = matcher.group(2).replace('_', '.');
        } else {
            baseName = simpleName;
            baseVersion = null;
        }
        final Schema schema = schemaAnnotation.getAnnotation(Schema.class);
        final String[] namespaces = schema.compatibilityNamespaces();
        final Schema.Kind kind = schema.kind();
        final String namespace;
        final String version;
        if (schema.namespace().isEmpty()) {
            namespace = namespaceify(baseName);
        } else {
            namespace = schema.namespace();
        }
        if (schema.version().isEmpty()) {
            if (baseVersion == null) {
                version = "1.0";
            } else {
                version = baseVersion;
            }
        } else {
            version = schema.version();
        }
        final String schemaLocation = schema.schemaLocation();
        final String xmlNamespace = buildNamespace(kind, namespace, version);
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
        return new NewSchemaInfo(version, namespace, kind, schemaLocation, schemaLocationFileName, namespaces, rootResources, AnnotationUtils.isLocalSource(this, schemaAnnotation), xmlNamespace);
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
        return new RootResourceInfo(name, type, provides == null ? NO_STRINGS : provides.value(), xmlName == null ? name : xmlName.value(), resourceInfo);
    }

    public ResourceInfo processResource(final TypeElement resourceInterface) {
        if (resourceInfoMap.containsKey(resourceInterface)) {
            return resourceInfoMap.get(resourceInterface);
        }
        ResourceInfo info;
        resourceInfoMap.put(resourceInterface, info = processNewResource(resourceInterface));
        return info;
    }

    private static Set<String> skipMemberClassNames = new HashSet<String>(Arrays.asList(Object.class.getName(), Resource.class.getName()));

    private ResourceMember processResourceMember(final ExecutableElement element) {
        if (skipMemberClassNames.contains(((TypeElement) element.getEnclosingElement()).getQualifiedName().toString())) {
            return null;
        }
        if (element.getAnnotation(Attribute.class) != null) {
            return processAttribute(element);
        } else if (element.getAnnotation(AttributeGroup.class) != null) {
            return processAttributeGroup(element.getReturnType());
        } else if (getAnnotation(env.getElementUtils(), element, SubResource.class.getName()) != null) {
            return processSubResource(element);
        } else if (affiliatedMethods.contains(element)) {
            // it's OK to skip
            return null;
        } else {
            env.getMessager().printMessage(ERROR, "Unknown member '" + element + "' on resource " + element.getEnclosingElement(), element);
            return null;
        }
    }

    private ResourceInfo processNewResource(final TypeElement resourceInterface) {
        final List<ResourceMember> members = new ArrayList<ResourceMember>();
        for (Element element : env.getElementUtils().getAllMembers(resourceInterface)) {
            if (element.getKind() == ElementKind.METHOD) {
                final ResourceMember member = processResourceMember((ExecutableElement) element);
                if (member != null) {
                    members.add(member);
                }
            }
        }
        return new ResourceInfo(resourceInterface, members.toArray(new ResourceMember[members.size()]));
    }

    public SubResourceInfo processSubResource(final ExecutableElement executableElement) {
        if (((TypeElement) executableElement.getEnclosingElement()).getQualifiedName().toString().equals(Object.class.getName())) {
            return null;
        }
        if (subResourceInfoMap.containsKey(executableElement)) {
            return subResourceInfoMap.get(executableElement);
        }
        SubResourceInfo info;
        subResourceInfoMap.put(executableElement, info = processNewSubResource(executableElement));
        return info;
    }

    private SubResourceInfo processNewSubResource(final ExecutableElement executableElement) {
        final Messager messager = env.getMessager();
        final String methodName = executableElement.getSimpleName().toString();
        final ExecutableElement collector;
        final ExecutableElement fetcher;
        final String propertyName;
        final TypeMirror returnType = executableElement.getReturnType();
        final TypeMirror resourceType;
        if (! methodName.startsWith("get")) {
            messager.printMessage(ERROR, "SubResource reference method must be a getter", executableElement);
            return null;
        }
        if (methodName.endsWith("Names")) {
            // looks like the reference collector
            final TypeMirror returnTypeErasure = env.getTypeUtils().erasure(returnType);
            if (!isSameType(env.getTypeUtils().erasure(getType(List.class)), returnTypeErasure)) {
                messager.printMessage(ERROR, "SubResource reference list getter must return a java.util.List (found " + returnTypeErasure + ")", executableElement);
                return null;
            }
            collector = executableElement;
            final List<? extends TypeMirror> typeArgs = ((DeclaredType) returnType).getTypeArguments();
            if (typeArgs.size() != 1) {
                messager.printMessage(ERROR, "SubResource reference list must have exactly one type argument", executableElement);
                return null;
            }
            if (! executableElement.getParameters().isEmpty()) {
                messager.printMessage(ERROR, "SubResource reference list getter must not accept any arguments", executableElement);
                return null;
            }
            if (! isSameType(String.class, typeArgs.get(0))) {
                messager.printMessage(ERROR, "SubResource reference list getter must return a list of Strings", executableElement);
                return null;
            }
            propertyName = methodName.substring(3, methodName.length() - 5);
            final String fetcherName = "get" + propertyName;

            ExecutableElement prospect = null;
            for (ExecutableElement otherMethod : ElementFilter.methodsIn(env.getElementUtils().getAllMembers((TypeElement) executableElement.getEnclosingElement()))) {
                if (skipMemberClassNames.contains(((TypeElement)otherMethod.getEnclosingElement()).getQualifiedName().toString())) {
                    continue;
                }
                final List<? extends VariableElement> parameters = otherMethod.getParameters();
                final String prospectiveName = otherMethod.getSimpleName().toString();
                if (prospectiveName.equals(fetcherName) && isAssignable(otherMethod.getReturnType(), Resource.class) && parameters.size() == 1 && isSameType(String.class, parameters.get(0).asType())) {
                    prospect = otherMethod;
                    break;
                }
            }
            if (prospect == null) {
                messager.printMessage(ERROR, "SubReference must have a corresponding resource getter method which accepts a String and returns a valid resource type", executableElement);
                return null;
            }
            fetcher = prospect;
            resourceType = prospect.getReturnType();
        } else {
            // looks like the reference fetcher
            if (! isAssignable(returnType, Resource.class)) {
                messager.printMessage(ERROR, "SubResource must refer to a resource type, currently it is: "+returnType, executableElement);
                return null;
            }
            fetcher = executableElement;
            resourceType = executableElement.getReturnType();

            final List<? extends VariableElement> fetcherParameters = executableElement.getParameters();
            if (fetcherParameters.size() != 1 || ! isSameType(String.class, fetcherParameters.get(0).asType())) {
                messager.printMessage(ERROR, "SubReference resource getter method must accept a String and return a " + resourceType, executableElement);
                return null;
            }

            propertyName = methodName.substring(3);
            final String collectorName = "get" + propertyName + "Names";

            ExecutableElement prospect = null;
            for (ExecutableElement otherMethod : ElementFilter.methodsIn(env.getElementUtils().getAllMembers((TypeElement) executableElement.getEnclosingElement()))) {
                final List<? extends VariableElement> parameters = otherMethod.getParameters();
                if (otherMethod.getSimpleName().toString().equals(collectorName) && isSameType(List.class, env.getTypeUtils().erasure(otherMethod.getReturnType())) && parameters.size() == 0) {
                    final DeclaredType listType = (DeclaredType) otherMethod.getReturnType();
                    final List<? extends TypeMirror> typeArguments = listType.getTypeArguments();
                    if (typeArguments.size() != 1 || ! isSameType(String.class, typeArguments.get(0))) {
                        messager.printMessage(ERROR, "SubResource reference list getter must return a java.util.List<String>", executableElement);
                        return null;
                    }
                    prospect = otherMethod;
                }
            }
            if (prospect == null) {
                messager.printMessage(ERROR, "SubReference must have a corresponding resource getter method which accepts a String and returns a valid resource type", executableElement);
                return null;
            }
            collector = prospect;
        }
        final AnnotationMirror subResourceAnnotation = getAnnotation(env.getElementUtils(), executableElement, SubResource.class.getName());
        final String type = stringValue(getAnnotationValue(subResourceAnnotation, "type"));
        final String name = def(stringValue(getAnnotationValue(subResourceAnnotation, "name")), xmlify(xmlify(propertyName)));
        final boolean requiresUnique = booleanValue(getAnnotationValue(subResourceAnnotation, "requiresUniqueProvider"), false);
        final AnnotationValue childrenValue = getAnnotationValue(subResourceAnnotation, "children");
        final TypeMirror[] children = classArrayValue(childrenValue);
        final List<ResourceInfo> childResources = new ArrayList<ResourceInfo>();
        affiliatedMethods.add(collector);
        affiliatedMethods.add(fetcher);
        if (children != null) {
            for (TypeMirror child : children) {
                if (! (child instanceof DeclaredType && ((DeclaredType)child).asElement().getKind() == ElementKind.INTERFACE)) {
                    messager.printMessage(ERROR, "SubResource children must be interfaces", executableElement, subResourceAnnotation, childrenValue);
                } else {
                    final ResourceInfo info = processResource((TypeElement) ((DeclaredType) child).asElement());
                    if (info != null) {
                        childResources.add(info);
                    }
                }
            }
        }
        return new SubResourceInfo(collector, fetcher, resourceType, type, name, requiresUnique, childResources.toArray(new ResourceInfo[childResources.size()]));
    }

    public AttributeValueInfo processAttributeValue(String name, ExecutableElement declaringElement) {
        if (skipMemberClassNames.contains(((TypeElement) declaringElement.getEnclosingElement()).getQualifiedName().toString())) {
            return null;
        }
        if (attributeValueInfoMap.containsKey(declaringElement)) {
            return attributeValueInfoMap.get(declaringElement);
        }
        final AttributeValueInfo info = processNewAttributeValue(name, declaringElement);
        attributeValueInfoMap.put(declaringElement, info);
        return info;
    }

    private AttributeValueInfo processNewAttributeValue(String name, ExecutableElement declaringElement) {

        final Messager messager = getEnv().getMessager();
        final TypeMirror type = declaringElement.getReturnType();

        if (type.getKind() == TypeKind.VOID || type.getKind() == TypeKind.NONE) {
            messager.printMessage(ERROR, "Attribute methods must have a non-void return value on " + declaringElement, declaringElement);
            return null;
        }

        String simpleName = declaringElement.getSimpleName().toString();
        boolean getter = simpleName.startsWith("get");

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
                final AnnotationValue referenceTypeAnnotationValue = getAnnotationValue(annotationMirror, "referenceType");
                TypeMirror expectedReferenceType = classValue(referenceTypeAnnotationValue);
                if (expectedReferenceType == null) {
                    expectedReferenceType = declaringElement.getReturnType();
                }
                if (! (expectedReferenceType instanceof DeclaredType && ((DeclaredType)expectedReferenceType).asElement().getKind() == ElementKind.INTERFACE)) {
                    messager.printMessage(ERROR, "Reference types must refer to Resource interfaces", declaringElement, annotationMirror, referenceTypeAnnotationValue);
                    return null;
                }
                referenceType = (DeclaredType) expectedReferenceType;
                monitor = booleanValue(getAnnotationValue(annotationMirror, "monitor"), false);
            }
        }

        final TypeElement enclosingElement = (TypeElement) declaringElement.getEnclosingElement();

        for (VariableElement fieldElement : ElementFilter.fieldsIn(env.getElementUtils().getAllMembers(enclosingElement))) {
            if (defaultVarName.equals(fieldElement.getSimpleName().toString())) {
                if (! isAssignable(fieldElement.asType(), type)) {
                    messager.printMessage(ERROR, "Cannot assign default property value to property of type " + type, fieldElement);
                } else {
                    defaultVal = fieldElement;
                }
                break;
            }
        }

        if (referenceType != null) {
            if (enumerations != null) {
                messager.printMessage(WARNING, "Enumerations will not be used for attribute of type " + type, declaringElement);
            }
            if (defaultVal != null) {
                messager.printMessage(WARNING, "Default value will not be used for attribute group of type " + type, defaultVal);
            }
            if (! getter) {
                messager.printMessage(ERROR, "Reference attribute must be a getter method", declaringElement);
                return null;
            }
            // find getXXXName method...
            ExecutableElement getNameMethod = null;
            for (ExecutableElement executableElement : ElementFilter.methodsIn(env.getElementUtils().getAllMembers(enclosingElement))) {
                if (executableElement.getSimpleName().toString().equals(simpleName + "Name")) {
                    if (executableElement.getParameters().size() > 0) {
                        continue;
                    }
                    if (! isSameType(String.class, executableElement.getReturnType())) {
                        messager.printMessage(ERROR, "getXXXName method must return a String", executableElement);
                        return null;
                    }
                    getNameMethod = executableElement;
                    break;
                }
            }
            if (getNameMethod == null) {
                messager.printMessage(WARNING, "No getXXXName method for reference attribute", declaringElement);
            }
            return new ReferenceAttributeValueInfo(name, declaringElement, getNameMethod, required, referenceType, monitor);
        }

        if (isAssignable(type, String.class)) {
            return new StringAttributeValueInfo(name, defaultVal, required, enumerations);
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
                messager.printMessage(ERROR, "Attribute group must not be a resource", typeElement);
                return null;
            }

            final ArrayList<ResourceMember> resourceMembers = new ArrayList<ResourceMember>();

            for (ExecutableElement executableElement : ElementFilter.methodsIn(env.getElementUtils().getAllMembers(typeElement))) {
                final ResourceMember member = processResourceMember(executableElement);
                if (member != null) {
                    resourceMembers.add(member);
                }
            }

            final AttributeGroupInfo info = new AttributeGroupInfo(typeElement, resourceMembers.toArray(new ResourceMember[resourceMembers.size()]));
            addAttributeGroupInfo(info);
            return info;
        } finally {
            finishAttributeGroup(typeElement);
        }
    }

    public AttributeInfo processAttribute(ExecutableElement executableElement) {
        if (((TypeElement) executableElement.getEnclosingElement()).getQualifiedName().toString().equals(Object.class.getName())) {
            return null;
        }
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
        boolean wrapperElement = true;
        XmlRender.As renderAs = XmlRender.As.ELEMENT;
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
            } else if (annotationIs(annotationMirror, XmlRender.class)) {
                wrapperElement = booleanValue(getAnnotationValue(annotationMirror, "wrapperElement"), true);
                renderAs = enumConstValue(XmlRender.As.class, getAnnotationValue(annotationMirror, "as"));
            }
        }

        final AttributeValueInfo valueInfo = processAttributeValue(name, executableElement);
        if (valueInfo == null) {
            return null;
        }
        return new AttributeInfo(executableElement, name, valueInfo, access, virtual, required, defaultVal, validators, xmlName, wrapperElement, renderAs);
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
                    if (isAssignable(validatorType, AttributeValidator.class)) {
                        validatorsList.add(validatorType);
                    } else {
                        messager.printMessage(ERROR, "Validator type " + validatorType + " does not implement " + AttributeValidator.class, typeElement, attributeTypeMirror);
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
                if (info != null) {
                    final AttributeValueInfo valueInfo = info.getValueInfo();
                    if (! valueInfo.isValidInAttributeType()) {
                        messager.printMessage(ERROR, "Attribute with this type cannot be used in an AttributeType", executableElement);
                    } else {
                        attributeInfoList.add(info);
                    }
                }
            }

            final AttributeTypeInfo info = new AttributeTypeInfo(typeElement, implClassName, xmlTypeName, renderAs, wrapperElement, validators, attributeInfoList.toArray(new AttributeInfo[attributeInfoList.size()]));
            addAttributeTypeInfo(info);
            return info;
        } finally {
            finishAttributeType(typeElement);
        }
    }

    boolean isSameType(final Class<?> type1, final TypeMirror type2) {
        final Types types = env.getTypeUtils();
        return types.isSameType(types.erasure(getType(type1)), types.erasure(type2));
    }

    boolean isSameType(final TypeMirror type1, final TypeMirror type2) {
        return env.getTypeUtils().isSameType(type1, type2);
    }

    boolean isAssignable(final TypeMirror source, final TypeMirror target) {
        return env.getTypeUtils().isAssignable(source, target);
    }

    boolean isAssignable(final Class<?> source, final TypeMirror target) {
        final Types types = env.getTypeUtils();
        return isAssignable(types.erasure(getType(source)), types.erasure(target));
    }

    boolean isAssignable(final TypeMirror source, final Class<?> target) {
        final Types types = env.getTypeUtils();
        return isAssignable(types.erasure(source), types.erasure(getType(target)));
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
