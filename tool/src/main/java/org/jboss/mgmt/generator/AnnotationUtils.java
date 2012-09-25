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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class AnnotationUtils {

    private AnnotationUtils() {
    }

    public static AnnotationMirror getAnnotation(Elements elements, Element annotatedElement, String annotationName) {
        for (AnnotationMirror mirror : elements.getAllAnnotationMirrors(annotatedElement)) {
            if (mirror.getAnnotationType().asElement().getSimpleName().toString().equals(annotationName)) {
                return mirror;
            }
        }
        return null;
    }

    public static AnnotationValue getAnnotationValue(AnnotationMirror mirror, String name) {
        final Set<? extends Map.Entry<? extends ExecutableElement,? extends AnnotationValue>> entries = mirror.getElementValues().entrySet();
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : entries) {
            if (entry.getKey().getSimpleName().toString().equals(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static String getAnnotationValueString(AnnotationMirror mirror, String name) {
        return stringValue(getAnnotationValue(mirror, name));
    }

    public static String getAnnotationValueClassName(AnnotationMirror mirror, String name) {
        return classNameValue(getAnnotationValue(mirror, name));
    }

    public static String stringValue(AnnotationValue value) {
        if (value == null) return null;
        return (String) value.getValue();
    }

    public static TypeMirror classValue(AnnotationValue value) {
        if (value == null) return null;
        return (TypeMirror) value.getValue();
    }

    public static String classNameValue(AnnotationValue value) {
        if (value == null) return null;
        final TypeMirror typeMirror = classValue(value);
        return typeMirror instanceof PrimitiveType ? ((PrimitiveType)typeMirror).getKind().toString().toLowerCase(Locale.ENGLISH) : ((DeclaredType)typeMirror).asElement().getSimpleName().toString();
    }

    public static VariableElement enumValue(AnnotationValue value) {
        return (VariableElement) value;
    }

    public static String enumNameValue(AnnotationValue value) {
        final VariableElement element = enumValue(value);
        return element.getSimpleName().toString();
    }

    public static <E extends Enum<E>> E enumConstValue(Class<E> type, AnnotationValue value) {
        return Enum.valueOf(type, enumNameValue(value));
    }

    public static boolean booleanValue(AnnotationValue value, boolean defVal) {
        if (value == null) return defVal;
        return ((Boolean) value.getValue()).booleanValue();
    }

    public static int intValue(AnnotationValue value, int defVal) {
        if (value == null) return defVal;
        return ((Integer) value.getValue()).intValue();
    }

    public static String[] stringArrayValue(AnnotationValue value) {
        if (value == null) return null;
        @SuppressWarnings("unchecked")
        final List<? extends AnnotationValue> list = (List<? extends AnnotationValue>) value.getValue();
        final String[] array = new String[list.size()];
        int i = 0;
        for (AnnotationValue annotationValue : list) {
            array[i++] = stringValue(annotationValue);
        }
        return array;
    }

    public static TypeMirror[] classArrayValue(AnnotationValue value) {
        if (value == null) return null;
        @SuppressWarnings("unchecked")
        final List<? extends AnnotationValue> list = (List<? extends AnnotationValue>) value.getValue();
        final TypeMirror[] array = new TypeMirror[list.size()];
        int i = 0;
        for (AnnotationValue annotationValue : list) {
            array[i++] = classValue(annotationValue);
        }
        return array;
    }

    public static String[] classNameArrayValue(AnnotationValue value) {
        if (value == null) return null;
        @SuppressWarnings("unchecked")
        final List<? extends AnnotationValue> list = (List<? extends AnnotationValue>) value.getValue();
        final String[] array = new String[list.size()];
        int i = 0;
        for (AnnotationValue annotationValue : list) {
            array[i++] = classNameValue(annotationValue);
        }
        return array;
    }
}
