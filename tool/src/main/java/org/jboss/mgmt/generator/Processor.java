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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jboss.mgmt.Resource;
import org.jboss.mgmt.annotation.Access;
import org.jboss.mgmt.annotation.Attribute;
import org.jboss.mgmt.annotation.AttributeGroup;
import org.jboss.mgmt.annotation.AttributeType;
import org.jboss.mgmt.annotation.Provides;
import org.jboss.mgmt.annotation.ResourceType;
import org.jboss.mgmt.annotation.RootResource;
import org.jboss.mgmt.annotation.xml.XmlName;

import javax.annotation.processing.Completion;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementFilter;

import javax.tools.Diagnostic;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class Processor implements javax.annotation.processing.Processor {
    private ProcessingEnvironment env;

    public Set<String> getSupportedOptions() {
        return Collections.emptySet();
    }

    public Set<String> getSupportedAnnotationTypes() {
        return new HashSet<String>(Arrays.asList(
            AttributeType.class.getName(),
            ResourceType.class.getName(),
            RootResource.class.getName(),
            AttributeGroup.class.getName()
        ));
    }

    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    public void init(final ProcessingEnvironment processingEnv) {
        env = processingEnv;
    }

    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        final Messager messager = env.getMessager();
        final Session session = SessionFactory.openSession(env, roundEnv);
        final Set<? extends Element> attributeTypeElements = roundEnv.getElementsAnnotatedWith(AttributeType.class);
        final Set<? extends Element> resourceTypeElements = roundEnv.getElementsAnnotatedWith(ResourceType.class);
        final Set<? extends Element> rootResourceElements = roundEnv.getElementsAnnotatedWith(RootResource.class);
        for (TypeElement element : ElementFilter.typesIn(rootResourceElements)) {
            if (element.getKind() != ElementKind.INTERFACE) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Only interfaces may be annotated with " + RootResource.class, element);
            }
            if (! env.getTypeUtils().isAssignable(element.asType(), env.getElementUtils().getTypeElement(Resource.class.getName()).asType())) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Resource interfaces must extend " + Resource.class, element);
            }
            final List<? extends AnnotationMirror> mirrors = env.getElementUtils().getAllAnnotationMirrors(element);
            String version = "1.0";
            String xmlName = null;
            String[] provides = null;
            String resourceType = null;
            String resourceModelName = null;
            String resourceNamespace = null;
            String schemaLocation = null;
            String[] resourceCompatNamespaces = null;
            RootResource.Kind resourceKind = RootResource.Kind.EXTENSION;
            final Map<String, AnnotationMirror> mirrorMap = mirrorListToMap(mirrors);
            for (Map.Entry<String, AnnotationMirror> mirrorMapEntry : mirrorMap.entrySet()) {
                final String annotationName = mirrorMapEntry.getKey();
                final AnnotationMirror annotationMirror = mirrorMapEntry.getValue();
                final Map<String, AnnotationValue> valueMap = mirrorValuesToMap(annotationMirror);

                if (annotationName.equals(RootResource.class.getName())) {
                    if (valueMap.containsKey("type")) resourceType = valueMap.get("type").getValue().toString();
                    if (valueMap.containsKey("name")) resourceModelName = valueMap.get("name").getValue().toString();
                    if (valueMap.containsKey("version")) version = valueMap.get("version").getValue().toString();
                    if (valueMap.containsKey("kind")) resourceKind = getAnnotationValue(RootResource.Kind.class, valueMap.get("kind"));
                    if (valueMap.containsKey("namespace")) resourceNamespace = valueMap.get("namespace").getValue().toString();
                    if (valueMap.containsKey("compatibilityNamespaces")) resourceCompatNamespaces = getStringArrayAnnotationValue(valueMap.get("compatibilityNamespaces"));
                    if (valueMap.containsKey("schemaLocation")) schemaLocation = valueMap.get("schemaLocation").getValue().toString();
                } else if (annotationName.equals(XmlName.class.getName())) {
                    xmlName = valueMap.get("value").getValue().toString();
                } else if (annotationName.equals(Provides.class.getName())) {
                    provides = getStringArrayAnnotationValue(valueMap.get("value"));
                }
            }
            final RootResourceBuilder resourceBuilder = session.rootResource(element.getQualifiedName().toString(), (DeclaredType) element.asType(), version);
            if (xmlName != null) resourceBuilder.xmlName(xmlName);
            final String docComment = env.getElementUtils().getDocComment(element);
            if (docComment != null) resourceBuilder.description(docComment);
            if (provides != null) {
                for (String provide : provides) {
                    resourceBuilder.provides(provide);
                }
            }
            if (resourceType != null) {
                //
            }
            if (resourceModelName != null) {
                //
            }
            if (resourceNamespace != null) {
                resourceBuilder.namespace(resourceNamespace);
            }
            if (resourceCompatNamespaces != null) {
                //
            }
            if (resourceKind != null) {
                resourceBuilder.kind(resourceKind);
            }
            if (schemaLocation != null) {
                resourceBuilder.schemaLocation(schemaLocation);
            }

            for (ExecutableElement enclosedElement : ElementFilter.methodsIn(element.getEnclosedElements())) {
                final String methodName = enclosedElement.getSimpleName().toString();
                final String attributeName;
                if (methodName.startsWith("is")) {
                    attributeName = methodName.substring(2);
                } else if (methodName.startsWith("get")) {
                    attributeName = methodName.substring(3);
                } else {
                    attributeName = methodName;
                }
                final List<? extends AnnotationMirror> methodMirrors = env.getElementUtils().getAllAnnotationMirrors(enclosedElement);
                final Map<String, AnnotationMirror> methodMirrorMap = mirrorListToMap(methodMirrors);
                if (! methodMirrorMap.containsKey(Attribute.class.getName())) {
                    continue;
                }
                final AnnotationMirror attributeMirror = methodMirrorMap.get(Attribute.class.getName());
                final Map<String, AnnotationValue> valueMap = mirrorValuesToMap(attributeMirror);
                final AttributeBuilder<? extends RootResourceBuilder> attributeBuilder = resourceBuilder.attribute(attributeName);
                attributeBuilder.type(enclosedElement.getReturnType());
                attributeBuilder.access(getAnnotationValue(Access.class, valueMap.get("access")));
            }
        }
        session.generateSource();
        return true;
    }

    private static String[] getStringArrayAnnotationValue(final AnnotationValue value) {
        String[] values;
        @SuppressWarnings("unchecked")
        List<? extends AnnotationValue> subValues = (List<? extends AnnotationValue>) value.getValue();
        values = new String[subValues.size()];
        for (int idx = 0; idx < subValues.size(); idx++) {
            AnnotationValue annotationValue = subValues.get(idx);
            values[idx] = (String) annotationValue.getValue();
        }
        return values;
    }

    private static Map<String, AnnotationMirror> mirrorListToMap(List<? extends AnnotationMirror> mirrors) {
        if (mirrors.isEmpty()) return Collections.emptyMap();
        final LinkedHashMap<String, AnnotationMirror> map = new LinkedHashMap<String, AnnotationMirror>();
        for (AnnotationMirror mirror : mirrors) {
            final DeclaredType type = mirror.getAnnotationType();
            final TypeElement annotationTypeElement = (TypeElement) type.asElement();
            final String annotationName = annotationTypeElement.getQualifiedName().toString();
            map.put(annotationName, mirror);
        }
        return map;
    }

    private static Map<String, AnnotationValue> mirrorValuesToMap(AnnotationMirror mirror) {
        final LinkedHashMap<String, AnnotationValue> map = new LinkedHashMap<String, AnnotationValue>();
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror.getElementValues().entrySet()) {
            map.put(entry.getKey().getSimpleName().toString(), entry.getValue());
        }
        return map;
    }

    private static <E extends Enum<E>> E getAnnotationValue(Class<E> enumType, AnnotationValue value) {
        if (value == null) {
            return null;
        }
        return Enum.valueOf(enumType, ((VariableElement) value.getValue()).getSimpleName().toString());
    }

    public Iterable<? extends Completion> getCompletions(final Element element, final AnnotationMirror annotation, final ExecutableElement member, final String userText) {
        return Collections.emptyList();
    }
}
