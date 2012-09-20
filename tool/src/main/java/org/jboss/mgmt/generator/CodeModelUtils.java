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

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.sun.codemodel.ClassType;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JType;
import com.sun.codemodel.JTypeVar;

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
public final class CodeModelUtils {

    private CodeModelUtils() {
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

    private static JClass mirror(ProcessingEnvironment env, JCodeModel codeModel, TypeElement element) {
        if (true) {
            return codeModel.directClass(element.getQualifiedName().toString());
        }
        final Messager messager = env.getMessager();
        final Element enclosingElement = element.getEnclosingElement();
        JDefinedClass definedClass;
        if (enclosingElement instanceof TypeElement) {
            final JDefinedClass enclosing = (JDefinedClass) mirror(env, codeModel, (TypeElement) enclosingElement);
            final Iterator<JDefinedClass> iterator = enclosing.classes();
            while (iterator.hasNext()) {
                final JDefinedClass nested = iterator.next();
                if (nested.name().equals(element.getSimpleName().toString())) {
                    return nested;
                }
            }
            try {
                definedClass = enclosing._class(translateModifiers(element), element.getSimpleName().toString(), translateElementType(messager, element));
            } catch (JClassAlreadyExistsException e) {
                // should be impossible!
                throw new IllegalStateException(e);
            }
        } else {
            definedClass = codeModel._getClass(element.getQualifiedName().toString());
            if (definedClass != null) {
                return definedClass;
            }
            try {
                definedClass = codeModel._class(translateModifiers(element), element.getQualifiedName().toString(), translateElementType(messager, element));
            } catch (JClassAlreadyExistsException e) {
                // should be impossible!
                throw new IllegalStateException(e);
            }
        }
        definedClass.hide();
        for (Element memberElement : element.getEnclosedElements()) {
            final String simpleName = memberElement.getSimpleName().toString();
            switch (memberElement.getKind()) {
                case ENUM:
                case CLASS:
                case ANNOTATION_TYPE:
                case INTERFACE:
                    mirror(env, codeModel, (TypeElement) memberElement);
                    break;
                case ENUM_CONSTANT:
                    // we don't really care about the parameters or anything like that...
                    definedClass.enumConstant(simpleName);
                    break;
                case FIELD:
                    definedClass.field(translateModifiers(memberElement), typeFor(env, codeModel, variableType((VariableElement) memberElement)), simpleName);
                    break;
                case METHOD: {
                    ExecutableElement executableElement = (ExecutableElement) memberElement;
                    final JMethod method = definedClass.method(translateModifiers(memberElement), typeFor(env, codeModel, returnType(executableElement)), simpleName);
                    for (VariableElement parameter : executableElement.getParameters()) {
                        method.param(translateModifiers(parameter), typeFor(env, codeModel, variableType(parameter)), parameter.getSimpleName().toString());
                    }
                    break;
                }
                case CONSTRUCTOR: {
                    ExecutableElement executableElement = (ExecutableElement) memberElement;
                    final JMethod method = definedClass.constructor(translateModifiers(memberElement));
                    for (VariableElement parameter : executableElement.getParameters()) {
                        method.param(translateModifiers(parameter), typeFor(env, codeModel, variableType(parameter)), parameter.getSimpleName().toString());
                    }
                    break;
                }
                case STATIC_INIT:
                    // skip
                    break;
                case INSTANCE_INIT: {
                    // skip
                    break;
                }
                case TYPE_PARAMETER:
                    TypeParameterElement typeParameterElement = (TypeParameterElement) memberElement;
                    final List<? extends TypeMirror> bounds = typeParameterElement.getBounds();
                    if (bounds.isEmpty()) {
                        definedClass.generify(simpleName);
                    } else {
                        definedClass.generify(simpleName).bound((JClass) typeFor(env, codeModel, bounds.get(0)));
                    }
                    break;
                case OTHER:
                    // ???
                    break;
            }
        }
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

    public static JType typeFor(ProcessingEnvironment env, JCodeModel codeModel, TypeMirror typeMirror) {
        final Messager messager = env.getMessager();
        final TypeKind kind = typeMirror.getKind();
        switch (kind) {
            case BOOLEAN:
                return codeModel.BOOLEAN;
            case BYTE:
                return codeModel.BYTE;
            case SHORT:
                return codeModel.SHORT;
            case INT:
                return codeModel.INT;
            case LONG:
                return codeModel.LONG;
            case CHAR:
                return codeModel.CHAR;
            case FLOAT:
                return codeModel.FLOAT;
            case DOUBLE:
                return codeModel.DOUBLE;
            case VOID:
                return codeModel.VOID;
            case NULL:
                return codeModel.NULL;
            case ARRAY:
                return typeFor(env, codeModel, ((ArrayType) typeMirror).getComponentType()).array();
            case DECLARED:
            case ERROR:
                final DeclaredType declaredType = (DeclaredType) typeMirror;
                final JClass directClass = mirror(env, codeModel, (TypeElement) declaredType.asElement());
//                final List<? extends TypeMirror> arguments = declaredType.getTypeArguments();
//                if (arguments.isEmpty()) {
                // return raw type for now - todo
                    return directClass;
//                }
//                final JClass[] params = new JClass[arguments.size()];
//                for (int i = 0; i < params.length; i ++) {
//                    params[i] = (JClass) typeFor(messager, codeModel, arguments.get(i));
//                }
//                return directClass.erasure().narrow(params);
            case TYPEVAR:
                final TypeVariable typeVariable = (TypeVariable) typeMirror;
                final TypeMirror upperBound = typeVariable.getUpperBound();
                final Element element = typeVariable.asElement();
                final String varName = element.getSimpleName().toString();
                final Element enclosingElement = element.getEnclosingElement();
                JTypeVar typeVar;
                if (enclosingElement.getKind() == ElementKind.METHOD) {
                    // type var on a method...
                    messager.printMessage(Diagnostic.Kind.ERROR, "Can't process type var on method (yet) " + enclosingElement.getKind(), element);
                    return codeModel.wildcard();
                } else if (enclosingElement instanceof TypeElement) {
                    final JClass definedClass = mirror(env, codeModel, (TypeElement) enclosingElement);
                    typeVar = null;
                    for (JTypeVar typeParam : definedClass.typeParams()) {
                        if (typeParam.name().equals(varName)) {
                            typeVar = typeParam;
                        }
                    }
                    if (typeVar == null) {
                        messager.printMessage(Diagnostic.Kind.ERROR, "Type variable " + varName + " not found", element);
                        return codeModel.wildcard();
                    }
                } else {
                    messager.printMessage(Diagnostic.Kind.ERROR, "Unknown type variable enclosing element type " + enclosingElement.getKind());
                    return codeModel.wildcard();
                }
                if (upperBound != null) {
                    final List<? extends TypeMirror> superTypes = env.getTypeUtils().directSupertypes(upperBound);
                    for (TypeMirror type : superTypes) {
                        typeVar.bound((JClass) typeFor(env, codeModel, type));
                    }
                }
                return typeVar;
            case WILDCARD:
                final WildcardType wildcardType = (WildcardType) typeMirror;
                final TypeMirror extendsBound = wildcardType.getExtendsBound();
                final TypeMirror superBound = wildcardType.getSuperBound();
                if (extendsBound != null) {
                    return ((JClass) typeFor(env, codeModel, extendsBound)).wildcard();
                } else if (superBound != null) {
                    messager.printMessage(Diagnostic.Kind.ERROR, "Super wildcards not yet supported by codemodel...");
                    return codeModel.wildcard();
                } else {
                    return codeModel.wildcard();
                }
            default:
                messager.printMessage(Diagnostic.Kind.ERROR, "Translating invalid type " + kind + " of " + typeMirror);
                return codeModel.wildcard();
        }
    }
}
