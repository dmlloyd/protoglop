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

import java.util.Collections;
import java.util.Set;
import org.wildfly.core.management.annotation.CompositeOperation;
import org.wildfly.core.management.annotation.Schema;

import javax.annotation.processing.Completion;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

import javax.tools.Diagnostic;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class Processor implements javax.annotation.processing.Processor {

    private ProcessingEnvironment env;
    private GeneratorContext genCtxt;
    private ProcessingContext ctxt;

    public Set<String> getSupportedOptions() {
        return Collections.emptySet();
    }

    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(Schema.class.getName());
    }

    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    public void init(final ProcessingEnvironment processingEnv) {
        env = processingEnv;
    }

    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        final Messager messager = env.getMessager();
        final TypeElement schemaElement = env.getElementUtils().getTypeElement(Schema.class.getName());
        if (annotations.contains(schemaElement)) {
            ProcessingContext ctxt = new ProcessingContext(env, roundEnv);

            for (TypeElement typeElement : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(Schema.class))) {
                if (typeElement.getKind() == ElementKind.ANNOTATION_TYPE) {
                    messager.printMessage(Diagnostic.Kind.NOTE, "Processing schema for " + typeElement);
                    ctxt.processSchema(typeElement);
                }
            }
            for (TypeElement typeElement : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(CompositeOperation.class))) {
                if (typeElement.getKind() == ElementKind.INTERFACE) {
                    messager.printMessage(Diagnostic.Kind.ERROR, "Composite operations are not yet supported", typeElement);
                    if (false) ctxt.processCompositeOperation(typeElement);
                } else {
                    messager.printMessage(Diagnostic.Kind.WARNING, "Ignoring @CompositeOperation annotation on wrong element type", typeElement);
                }
            }
            if (genCtxt == null) genCtxt = new GeneratorContext(ctxt);
            genCtxt.generate();
        }
        return true;
    }

    public Iterable<? extends Completion> getCompletions(final Element element, final AnnotationMirror annotation, final ExecutableElement member, final String userText) {
        return Collections.emptyList();
    }
}
