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

import org.jboss.mgmt.annotation.Access;

import com.sun.codemodel.ClassType;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JDocComment;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JTypeVar;

import javax.annotation.processing.Messager;

import static com.sun.codemodel.ClassType.*;
import static com.sun.codemodel.JMod.*;
import static javax.tools.Diagnostic.Kind.*;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ResourceBuilderInterfaceGenerator extends AbstractClassGenerator {
    private final GeneralResourceBuilderImpl<?> resourceBuilder;

    public ResourceBuilderInterfaceGenerator(final JCodeModel codeModel, final Messager messager, final String className, final String packageName, final GeneralResourceBuilderImpl builder) {
        super(codeModel, messager, className, packageName, INTERFACE, PUBLIC);
        resourceBuilder = builder;
    }

    protected void populate(final JDefinedClass builderInterface) {
        final JCodeModel codeModel = getCodeModel();

    }
}
