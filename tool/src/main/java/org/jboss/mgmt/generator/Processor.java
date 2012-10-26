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
import java.util.Set;
import org.jboss.mgmt.annotation.AttributeGroup;
import org.jboss.mgmt.annotation.AttributeType;
import org.jboss.mgmt.annotation.ResourceType;
import org.jboss.mgmt.annotation.RootResource;
import org.jboss.mgmt.annotation.Schema;

import javax.annotation.processing.Completion;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

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
            Schema.class.getName(),
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
        final ProcessingContext ctxt = new ProcessingContext(env, roundEnv);
        final GeneratorContext genCtxt = new GeneratorContext(ctxt);
        final Set<NewSchemaInfo> schemas = new HashSet<NewSchemaInfo>();

        for (TypeElement typeElement : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(Schema.class))) {
            if (typeElement.getKind() == ElementKind.ANNOTATION_TYPE) {
                final NewSchemaInfo schemaInfo = ctxt.processSchema(typeElement);
                if (schemaInfo.isLocalSource()) {
                    genCtxt.doGenerate(schemaInfo);
                }
            }
        }

        genCtxt.generate();
        return true;
    }

    public Iterable<? extends Completion> getCompletions(final Element element, final AnnotationMirror annotation, final ExecutableElement member, final String userText) {
        return Collections.emptyList();
    }
}
