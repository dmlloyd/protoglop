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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
import org.jboss.mgmt.Commentable;
import org.jboss.mgmt.Resource;
import org.jboss.mgmt.ResourceRef;
import org.jboss.mgmt.annotation.Access;
import org.jboss.mgmt.annotation.Attribute;
import org.jboss.mgmt.annotation.AttributeGroup;
import org.jboss.mgmt.annotation.AttributeType;
import org.jboss.mgmt.annotation.Enumerated;
import org.jboss.mgmt.annotation.Provides;
import org.jboss.mgmt.annotation.Reference;
import org.jboss.mgmt.annotation.Required;
import org.jboss.mgmt.annotation.ResourceOperation;
import org.jboss.mgmt.annotation.ResourceType;
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
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
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

    private final Map<TypeElement, AttributeTypeInfo> attributeTypeInfoMap = new HashMap<TypeElement, AttributeTypeInfo>();
    private final Map<ExecutableElement, AttributeValueInfo> attributeValueInfoMap = new HashMap<ExecutableElement, AttributeValueInfo>();
    private final Map<TypeElement, AttributeGroupInfo> attributeGroupInfoMap = new HashMap<TypeElement, AttributeGroupInfo>();
    private final Map<TypeElement, SchemaInfo> schemaInfoMap = new HashMap<TypeElement, SchemaInfo>();
    private final Map<TypeElement, RootResourceInfo> rootResourceInfoMap = new HashMap<TypeElement, RootResourceInfo>();
    private final Map<ExecutableElement, SubResourceInfo> subResourceInfoMap = new HashMap<ExecutableElement, SubResourceInfo>();
    private final Map<TypeElement, ResourceInfo> resourceInfoMap = new HashMap<TypeElement, ResourceInfo>();
    private final Map<TypeElement, ResourceTypeInfo> resourceTypeInfoMap = new HashMap<TypeElement, ResourceTypeInfo>();
    private final Map<TypeElement, OperationInfo> operationInfoMap = new HashMap<TypeElement, OperationInfo>();
    private final Map<TypeElement, CompositeOperationInfo> compositeOperationInfoMap = new HashMap<TypeElement, CompositeOperationInfo>();

    private final Set<TypeElement> inFlightAttributeTypes = Collections.newSetFromMap(new IdentityHashMap<TypeElement, Boolean>());
    private final Set<TypeElement> inFlightAttributeGroups = Collections.newSetFromMap(new IdentityHashMap<TypeElement, Boolean>());

    ProcessingContext(final ProcessingEnvironment env, final RoundEnvironment roundEnv) {
        this.env = env;
        this.roundEnv = roundEnv;
    }

    // =======================================================
    //
    //     Processing methods
    //
    // =======================================================

    public Collection<SchemaInfo> getSchemas() {
        return schemaInfoMap.values();
    }

    public SchemaInfo processSchema(final TypeElement schemaElement) {
        if (schemaInfoMap.containsKey(schemaElement)) {
            return schemaInfoMap.get(schemaElement); // may be {@code null}
        }
        final SchemaInfo info;
        schemaInfoMap.put(schemaElement, info = processNewSchema(schemaElement));
        return info;
    }

    private static final Pattern NAME_WITH_VERSION = Pattern.compile("^([a-zA-Z_][a-zA-Z0-9_]+)_(\\d+(?:_\\d+)*)$");

    private SchemaInfo processNewSchema(final TypeElement schemaAnnotation) {
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
        return new SchemaInfo(version, namespace, kind, schemaLocation, schemaLocationFileName, namespaces, rootResources, AnnotationUtils.isLocalSource(this, schemaAnnotation), xmlNamespace);
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
        return new RootResourceInfo(resourceInterface, name, type, provides == null ? NO_STRINGS : provides.value(), xmlName == null ? name : xmlName.value(), resourceInfo);
    }

    public ResourceInfo processResource(final TypeElement resourceInterface) {
        if (resourceInfoMap.containsKey(resourceInterface)) {
            return resourceInfoMap.get(resourceInterface);
        }
        ResourceInfo info;
        resourceInfoMap.put(resourceInterface, info = processNewResource(resourceInterface));
        return info;
    }

    private ResourceInfo processNewResource(final TypeElement resourceInterface) {
        final List<ResourceMember> members = new ArrayList<ResourceMember>();
        final Elements elements = env.getElementUtils();
        for (Element element : elements.getAllMembers(resourceInterface)) {
            if (element.getKind() == ElementKind.METHOD) {
                final ResourceMember member = processResourceMember((ExecutableElement) element);
                if (member != null) {
                    members.add(member);
                }
            }
        }
        final String resourceName = NameUtils.fieldify(resourceInterface.getSimpleName().toString());
        String xmlName = AnnotationUtils.getXmlName(elements, resourceInterface);
        String xmlTypeName = AnnotationUtils.getXmlTypeName(elements, resourceInterface, xmlName);
        return new ResourceInfo(resourceInterface, members.toArray(new ResourceMember[members.size()]), resourceName, xmlName, xmlTypeName);
    }

    private static Set<String> skipMemberClassNames = new HashSet<String>(Arrays.asList(Object.class.getName(), Resource.class.getName(), Commentable.class.getName()));

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
        } else {
            env.getMessager().printMessage(ERROR, "Unknown member '" + element + "' on resource " + element.getEnclosingElement(), element);
            return null;
        }
    }

    public SubResourceInfo processSubResource(final ExecutableElement executableElement) {
        if (((TypeElement) executableElement.getEnclosingElement()).getQualifiedName().toString().equals(Object.class.getName())) {
            return null;
        }
        if (subResourceInfoMap.containsKey(executableElement)) {
            return subResourceInfoMap.get(executableElement);
        }
        SubResourceInfo info;
        subResourceInfoMap.put(executableElement, null); // fill with null to avoid recursion
        subResourceInfoMap.put(executableElement, info = processNewSubResource(executableElement));
        return info;
    }

    private SubResourceInfo processNewSubResource(final ExecutableElement executableElement) {
        final Messager messager = env.getMessager();
        final String methodName = executableElement.getSimpleName().toString();
        final TypeMirror rawReturnType = executableElement.getReturnType();
        final DeclaredType returnType;
        final List<? extends TypeMirror> returnTypeArguments;
        final TypeMirror rawReturnTypeArgument;
        final DeclaredType nestedResourceType;
        final TypeElement resourceTypeElement;
        if (rawReturnType.getKind() != TypeKind.DECLARED
            || ! isSameType(Map.class, env.getTypeUtils().erasure(returnType = ((DeclaredType)rawReturnType)))
            || (returnTypeArguments = returnType.getTypeArguments()).size() != 2
            || ! isSameType(String.class, returnTypeArguments.get(0))
            || (rawReturnTypeArgument = returnTypeArguments.get(1)).getKind() != TypeKind.DECLARED
            || (resourceTypeElement = (TypeElement)(nestedResourceType = ((DeclaredType)rawReturnTypeArgument)).asElement()).getKind() != ElementKind.INTERFACE
        ) {
            messager.printMessage(ERROR, "SubResource return type must be a Map of String to a valid resource type", executableElement);
            return null;
        }
        if (! methodName.startsWith("get")) {
            messager.printMessage(ERROR, "SubResource reference method must be a getter", executableElement);
            return null;
        }
        final String propertyName = methodName.substring(3);
        final AnnotationMirror subResourceAnnotation = getAnnotation(env.getElementUtils(), executableElement, SubResource.class.getName());
        final String type = stringValue(getAnnotationValue(subResourceAnnotation, "type"));
        final String name = def(stringValue(getAnnotationValue(subResourceAnnotation, "name")), xmlify(propertyName));
        final String xmlName = AnnotationUtils.getXmlName(env.getElementUtils(), executableElement, propertyName);
        final String xmlTypeName = AnnotationUtils.getXmlTypeName(env.getElementUtils(), executableElement, xmlName);

        final boolean requiresUnique = booleanValue(getAnnotationValue(subResourceAnnotation, "requiresUniqueProvider"), false);
        final AnnotationValue childrenValue = getAnnotationValue(subResourceAnnotation, "children");
        final TypeMirror[] children = classArrayValue(childrenValue);
        final List<ResourceInfo> childResources = new ArrayList<ResourceInfo>();
        final boolean hasChildren = children != null && children.length > 0;
        final boolean childIsResourceType;
        if (hasChildren) {
            if (getAnnotation(env.getElementUtils(), resourceTypeElement, ResourceType.class.getName()) == null) {
                messager.printMessage(ERROR, "If SubResource children are given, then the Map value type must be a @ResourceType", executableElement, subResourceAnnotation, childrenValue);
                return null;
            }
            childIsResourceType = true;
            for (TypeMirror child : children) {
                if (! (child instanceof DeclaredType && ((DeclaredType)child).asElement().getKind() == ElementKind.INTERFACE)) {
                    messager.printMessage(ERROR, "SubResource children must be interfaces", executableElement, subResourceAnnotation, childrenValue);
                } else if (! isAssignable(child, nestedResourceType)) {
                    messager.printMessage(ERROR, "SubResource type must be assignable from the declared resource type (map value type)", executableElement, subResourceAnnotation, childrenValue);
                } else {
                    final ResourceInfo info = processResource((TypeElement) ((DeclaredType) child).asElement());
                    if (info != null) {
                        childResources.add(info);
                    }
                }
            }
        } else {
            childIsResourceType = getAnnotation(env.getElementUtils(), resourceTypeElement, ResourceType.class.getName()) != null;
        }
        if (childIsResourceType) {
            return new SubResourceInfo(processResourceType(resourceTypeElement), type, name, xmlName, xmlTypeName, requiresUnique, childResources.toArray(new ResourceInfo[childResources.size()]));
        } else {
            final ResourceInfo resourceInfo = processResource(resourceTypeElement);
            return resourceInfo == null ? null : new SubResourceInfo(null, type, name, xmlName, xmlTypeName, requiresUnique, new ResourceInfo[] { resourceInfo });
        }
    }

    public ResourceTypeInfo processResourceType(final TypeElement typeElement) {
        if (resourceTypeInfoMap.containsKey(typeElement)) {
            return resourceTypeInfoMap.get(typeElement);
        }
        ResourceTypeInfo info;
        resourceTypeInfoMap.put(typeElement, null);
        resourceTypeInfoMap.put(typeElement, info = processNewResourceType(typeElement));
        return info;
    }

    private ResourceTypeInfo processNewResourceType(final TypeElement typeElement) {
        final AnnotationMirror resourceTypeAnnotation = getAnnotation(env.getElementUtils(), typeElement, ResourceType.class.getName());
        if (resourceTypeAnnotation == null) {
            env.getMessager().printMessage(ERROR, "Resource types must have (or inherit) a @ResourceType annotation", typeElement);
            return null;
        }
        final String name = stringValue(getAnnotationValue(resourceTypeAnnotation, "name"));
        return new ResourceTypeInfo(typeElement, name);
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

        final TypeElement enclosingElement = (TypeElement) declaringElement.getEnclosingElement();

        for (VariableElement fieldElement : ElementFilter.fieldsIn(env.getElementUtils().getAllMembers(enclosingElement))) {
            if (defaultVarName.equals(fieldElement.getSimpleName().toString())) {
                final TypeMirror fieldType = fieldElement.asType();
                if (! isAssignable(fieldType, type)) {
                    messager.printMessage(ERROR, "Cannot assign default value of type " + fieldType + " to property of type " + type, fieldElement);
                } else {
                    defaultVal = fieldElement;
                }
                break;
            }
        }

        for (AnnotationMirror annotationMirror : declaringElement.getAnnotationMirrors()) {
            if (annotationIs(annotationMirror, Enumerated.class)) {
                enumerations = stringArrayValue(getAnnotationValue(annotationMirror, "name"));
            } else if (annotationIs(annotationMirror, Required.class)) {
                required = booleanValue(getAnnotationValue(annotationMirror, "value"), true);
            } else if (annotationIs(annotationMirror, Reference.class)) {
                final DeclaredType resourceRefType;
                final boolean list;
                if (isSameType(ResourceRef.class, type)) {
                    resourceRefType = (DeclaredType) type;
                    list = false;
                } else if (isSameType(List.class, type)) {
                    final DeclaredType listType = ((DeclaredType) type);
                    final List<? extends TypeMirror> typeParameters = listType.getTypeArguments();
                    if (typeParameters.size() != 1) {
                        messager.printMessage(ERROR, "Reference type lists must have one type argument", declaringElement);
                        return null;
                    }
                    final TypeMirror typeMirror = typeParameters.get(0);
                    if (! isSameType(ResourceRef.class, typeMirror)) {
                        messager.printMessage(ERROR, "Reference type lists must be of type List<ResourceRef<...>>", declaringElement);
                        return null;
                    }
                    resourceRefType = (DeclaredType) typeMirror;
                    list = true;
                } else {
                    messager.printMessage(ERROR, "Reference types must return ResourceRef instances or lists thereof", declaringElement);
                    return null;
                }
                final List<? extends TypeMirror> typeArguments = resourceRefType.getTypeArguments();
                if (typeArguments.size() != 1) {
                    messager.printMessage(ERROR, "ResourceRef must have a type argument", declaringElement);
                    return null;
                }
                final TypeMirror typeMirror = typeArguments.get(0);
                final DeclaredType targetType;
                if (typeMirror.getKind() == TypeKind.WILDCARD) {
                    // List<ResourceRef<? extends Xyz>> or ResourceRef<? extends Xyz>
                    final WildcardType wildcardType = (WildcardType) typeMirror;
                    if (wildcardType.getSuperBound() != null) {
                        messager.printMessage(ERROR, "ResourceRef type variable cannot have a super bound", declaringElement);
                        return null;
                    }
                    final TypeMirror extendsBound = wildcardType.getExtendsBound();
                    if (extendsBound.getKind() != TypeKind.DECLARED) {
                        messager.printMessage(ERROR, "ResourceRef type variable must be or extend a concrete declared type", declaringElement);
                        return null;
                    }
                    targetType = (DeclaredType) extendsBound;
                } else if (typeMirror.getKind() == TypeKind.DECLARED) {
                    // List<ResourceRef<Xyz>> or ResourceRef<Xyz>
                    targetType = (DeclaredType) typeMirror;
                } else {
                    messager.printMessage(ERROR, "ResourceRef type variable must be or extend a concrete declared type", declaringElement);
                    return null;
                }
                monitor = booleanValue(getAnnotationValue(annotationMirror, "monitor"), false);
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
                return new ReferenceAttributeValueInfo(name, declaringElement, required, referenceType, monitor, list, targetType);
            }
        }

        if (isAssignable(type, String.class)) {
            return new StringAttributeValueInfo(name, defaultVal, required, enumerations);
        }

        if (enumerations != null) {
            messager.printMessage(WARNING, "Enumerations will not be used for attribute of type " + type, declaringElement);
        }

        if (type.getKind().isPrimitive()) {
            return new PrimitiveAttributeValueInfo(type.getKind(), name, defaultVal);
        }

        if (defaultVal != null) {
            messager.printMessage(WARNING, "Default value will not be used for attribute group of type " + type, defaultVal);
        }

        if (declaringElement.getAnnotation(AttributeGroup.class) != null) {
            if (isAssignable(type, Resource.class)) {
                messager.printMessage(ERROR, "AttributeGroup types cannot be Resources; to share a type, factor out a child interface which inherits the AttributeGroup type and Resource", declaringElement);
                return null;
            }
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

            final Elements elements = env.getElementUtils();
            for (ExecutableElement executableElement : ElementFilter.methodsIn(elements.getAllMembers(typeElement))) {
                final ResourceMember member = processResourceMember(executableElement);
                if (member != null) {
                    resourceMembers.add(member);
                }
            }
            String xmlName = AnnotationUtils.getXmlName(elements, typeElement);
            String xmlTypeName = AnnotationUtils.getXmlTypeName(elements, typeElement, xmlName);

            final AttributeGroupInfo info = new AttributeGroupInfo(xmlName, xmlTypeName, typeElement, resourceMembers.toArray(new ResourceMember[resourceMembers.size()]));
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
            name = getterName.substring(3);
        } else if (getterName.startsWith("is")) {
            name = getterName.substring(2);
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

    public CompositeOperationInfo processCompositeOperation(final TypeElement typeElement) {
        if (compositeOperationInfoMap.containsKey(typeElement)) {
            return compositeOperationInfoMap.get(typeElement);
        }
        CompositeOperationInfo info;
        compositeOperationInfoMap.put(typeElement, info = processNewCompositeOperation(typeElement));
        return info;
    }

    private CompositeOperationInfo processNewCompositeOperation(final TypeElement typeElement) {
        final Messager messager = env.getMessager();
        final ArrayList<OperationInfo> operationInfoList = new ArrayList<OperationInfo>();
        for (ExecutableElement executableElement : ElementFilter.methodsIn(env.getElementUtils().getAllMembers(typeElement))) {
            final TypeMirror returnType = executableElement.getReturnType();
            if (returnType instanceof DeclaredType) {
                final DeclaredType declaredType = (DeclaredType) returnType;
                final TypeElement returnTypeElement = (TypeElement) declaredType.asElement();
                for (AnnotationMirror annotationMirror : returnTypeElement.getAnnotationMirrors()) {
                    if (annotationIs(annotationMirror, ResourceOperation.class)) {
                        // todo blah blah
                    }
                }
                continue;
            }
            messager.printMessage(ERROR, "All members of composite operations must return operations or composite operations, or lists thereof", executableElement);
            return null;
        }
        return new CompositeOperationInfo();
    }

    // =======================================================
    //
    //     Utility methods
    //
    // =======================================================

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
