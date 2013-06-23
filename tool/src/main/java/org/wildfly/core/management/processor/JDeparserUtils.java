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

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jboss.jdeparser.ClassType;
import org.jboss.jdeparser.JClass;
import org.jboss.jdeparser.JClassAlreadyExistsException;
import org.jboss.jdeparser.JDeparser;
import org.jboss.jdeparser.JDefinedClass;
import org.jboss.jdeparser.JMod;
import org.jboss.jdeparser.JType;
import org.jboss.jdeparser.JTypeVar;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

import javax.tools.Diagnostic;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class JDeparserUtils {

    private JDeparserUtils() {
    }

    private static int translateModifiers(final Element element) {
        final Set<Modifier> modifiers = element.getModifiers();
        int mods = 0;
        for (Modifier modifier : modifiers) {
            switch (modifier) {
                case PUBLIC:
                    mods |= JMod.PUBLIC;
                    break;
                case PROTECTED:
                    if (element.getKind() != ElementKind.INTERFACE) {
                        // todo temp hack
                        mods |= JMod.PROTECTED;
                    }
                    break;
                case PRIVATE:
                    if (element.getKind() != ElementKind.INTERFACE) {
                        // todo temp hack
                        mods |= JMod.PRIVATE;
                    }
                    break;
                case ABSTRACT:
                    if (element.getKind() != ElementKind.INTERFACE) {
                        mods |= JMod.ABSTRACT;
                    }
                    break;
                case STATIC:
                    if (element.getKind() != ElementKind.INTERFACE) {
                        mods |= JMod.STATIC;
                    }
                    break;
                case FINAL:
                    mods |= JMod.FINAL;
                    break;
                case TRANSIENT:
                    mods |= JMod.TRANSIENT;
                    break;
                case VOLATILE:
                    mods |= JMod.VOLATILE;
                    break;
                case SYNCHRONIZED:
                    mods |= JMod.SYNCHRONIZED;
                    break;
                case NATIVE:
                    mods |= JMod.NATIVE;
                    break;
                case STRICTFP:
                    // ignore
                    break;
            }
        }
        return mods;
    }

    private static TypeMirror returnType(ExecutableElement executableElement) {
        return ((ExecutableType) executableElement.asType()).getReturnType();
    }

    private static TypeMirror variableType(VariableElement variableElement) {
        return variableElement.asType();
    }

    private static JDefinedClass mirror(ProcessingEnvironment env, JDeparser deparser, TypeElement element) {
        final String classQualifiedName = element.getQualifiedName().toString();
        final String classSimpleName = element.getSimpleName().toString();
        final Messager messager = env.getMessager();
        final Element enclosingElement = element.getEnclosingElement();
        JDefinedClass definedClass;
        if (enclosingElement instanceof TypeElement) {
            final JDefinedClass enclosing = mirror(env, deparser, (TypeElement) enclosingElement);
            final Iterator<JDefinedClass> iterator = enclosing.classes();
            while (iterator.hasNext()) {
                final JDefinedClass nested = iterator.next();
                if (nested.name().equals(classSimpleName)) {
                    return nested;
                }
            }
            try {
                definedClass = enclosing._class(translateModifiers(element), classSimpleName, translateElementType(messager, element));
            } catch (JClassAlreadyExistsException e) {
                // should be impossible!
                throw new IllegalStateException(e);
            }
        } else {
            definedClass = deparser._getClass(classQualifiedName);
            if (definedClass != null) {
                return definedClass;
            }
            try {
                definedClass = deparser._class(translateModifiers(element), classQualifiedName, translateElementType(messager, element));
            } catch (JClassAlreadyExistsException e) {
                // should be impossible!
                throw new IllegalStateException(e);
            }
        }
        // don't emit already emitted class
        definedClass.hide();
        // clone type parameters
        final List<? extends TypeParameterElement> typeParameters = element.getTypeParameters();
        for (TypeParameterElement typeParameter : typeParameters) {
            final String paramName = typeParameter.getSimpleName().toString();
            final List<? extends TypeMirror> bounds = typeParameter.getBounds();
            final JTypeVar typeVar = definedClass.generify(paramName);
            for (TypeMirror bound : bounds) {
                if (bound instanceof DeclaredType) {
                    final Element boundElement = ((DeclaredType) bound).asElement();
                    if (boundElement instanceof TypeElement) {
                        typeVar.bound(mirror(env, deparser, (TypeElement) boundElement));
                    } else {
                        messager.printMessage(Diagnostic.Kind.ERROR, "Expected element to be type element but it was a " + boundElement, boundElement);
                    }
                } else {
                    // type param?
                    messager.printMessage(Diagnostic.Kind.ERROR, "Expected bound to be type but it was a " + bound, element);
                }
            }
        }
        // clone extends/implements
        if (element.getKind() == ElementKind.CLASS) {
            final TypeMirror superclass = element.getSuperclass();
            if (superclass.getKind() != TypeKind.NONE) {
                if (superclass instanceof DeclaredType) {
                    final Element superclassElement = ((DeclaredType) superclass).asElement();
                    if (superclassElement instanceof TypeElement) {
                        definedClass._extends(mirror(env, deparser, (TypeElement) superclassElement));
                    } else {
                        messager.printMessage(Diagnostic.Kind.ERROR, "Expected superclass to be type element but it was a " + superclassElement, superclassElement);
                    }
                } else {
                    messager.printMessage(Diagnostic.Kind.ERROR, "Expected superclass to be a type but it was a " + superclass, element);
                }
            }
        }
        for (TypeMirror interf : element.getInterfaces()) {
            if (interf instanceof DeclaredType) {
                final Element interfElement = ((DeclaredType) interf).asElement();
                if (interfElement instanceof TypeElement) {
                    definedClass._implements(mirror(env, deparser, (TypeElement) interfElement));
                } else {
                    messager.printMessage(Diagnostic.Kind.ERROR, "Expected interface to be type element but it was a " + interfElement, interfElement);
                }
            } else {
                messager.printMessage(Diagnostic.Kind.ERROR, "Expected interface to be a type but it was a " + interf, element);
            }
        }
        // clone constructors, fields, and methods
        // clone annotations...?

        return definedClass;
    }

    private static ClassType translateElementType(final Messager messager, final TypeElement element) {
        switch (element.getKind()) {
            case ENUM:
                return ClassType.ENUM;
            case CLASS:
                return ClassType.CLASS;
            case ANNOTATION_TYPE:
                return ClassType.ANNOTATION_TYPE_DECL;
            case INTERFACE:
                return ClassType.INTERFACE;
        }
        messager.printMessage(Diagnostic.Kind.ERROR, "Cannot translate element type " + element.getKind(), element);
        return ClassType.CLASS;
    }

    public static JType typeFor(ProcessingEnvironment env, JDeparser deparser, TypeMirror typeMirror) {
        final Messager messager = env.getMessager();
        final TypeKind kind = typeMirror.getKind();
        switch (kind) {
            case BOOLEAN:
                return deparser.BOOLEAN;
            case BYTE:
                return deparser.BYTE;
            case SHORT:
                return deparser.SHORT;
            case INT:
                return deparser.INT;
            case LONG:
                return deparser.LONG;
            case CHAR:
                return deparser.CHAR;
            case FLOAT:
                return deparser.FLOAT;
            case DOUBLE:
                return deparser.DOUBLE;
            case VOID:
                return deparser.VOID;
            case NULL:
                return deparser.NULL;
            case ARRAY:
                return typeFor(env, deparser, ((ArrayType) typeMirror).getComponentType()).array();
            case DECLARED:
            case ERROR:
                final DeclaredType declaredType = (DeclaredType) typeMirror;
                final JDefinedClass mirrored = mirror(env, deparser, (TypeElement) declaredType.asElement());
                final List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
                final int argCount = typeArguments.size();
                if (argCount == 0) {
                    return mirrored;
                }
                final JClass[] arguments = new JClass[argCount];
                for (int idx = 0; idx < typeArguments.size(); idx++) {
                    TypeMirror mirror = typeArguments.get(idx);
                    arguments[idx] = (JClass) typeFor(env, deparser, mirror);
                }
                return mirrored.narrow(arguments);
            case WILDCARD:
                final WildcardType wildcardType = (WildcardType) typeMirror;
                final TypeMirror extendsBound = wildcardType.getExtendsBound();
                final TypeMirror superBound = wildcardType.getSuperBound();
                if (extendsBound != null) {
                    return ((JClass) typeFor(env, deparser, extendsBound)).wildcard();
                } else if (superBound != null) {
                    return ((JClass) typeFor(env, deparser, superBound)).superWildcard();
                } else {
                    return deparser.wildcard();
                }
            default:
                messager.printMessage(Diagnostic.Kind.ERROR, "Translating invalid type " + kind + " of " + typeMirror);
                return deparser.wildcard();
        }
    }
}
