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
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import com.sun.codemodel.JCodeModel;

import javax.tools.Diagnostic;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class GeneratorContext {

    private final ProcessingContext ctxt;
    private final JCodeModel codeModel = new JCodeModel();

    private final Set<NewSchemaInfo> generatedScheams = identityHashSet();

    private static <E> Set<E> identityHashSet() {
        return Collections.newSetFromMap(new IdentityHashMap<E, Boolean>());
    }

    GeneratorContext(final ProcessingContext ctxt) {
        this.ctxt = ctxt;
    }

    public JCodeModel getCodeModel() {
        return codeModel;
    }

    public void finish() {
        if (! ctxt.getRoundEnv().errorRaised()) {
            try {
                codeModel.build(new FilerCodeWriter(ctxt.getEnv().getFiler()));
            } catch (IOException e) {
                ctxt.getEnv().getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to generate code model: " + e);
            }
        }
    }

    public void generateSchema(final NewSchemaInfo schemaInfo) {
        if (generatedScheams.add(schemaInfo)) {

        }
    }
}
