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

package org.jboss.mgmt.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Meta-annotation for annotations for classes that are root resources for a given schema.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@Target(ANNOTATION_TYPE)
@Retention(SOURCE)
public @interface Schema {
    /**
     * The model version.
     *
     * @return the model version
     */
    String version();

    /**
     * The base namespace, e.g. "core.logging".  Namespaces starting with "core" and "jboss" are reserved
     * for use by JBoss.
     *
     * @return the namespace
     */
    String namespace();

    /**
     * The kind of root.  This affects generation of the XML namespace and XML and DMR parsers.
     *
     * @return the kind of root
     */
    Kind kind() default Kind.EXTENSION;

    /**
     * The schema location.  This URL string will be used to determine the target filename of the generated schema.
     *
     * @return the URL
     */
    String schemaLocation();

    /**
     * Compatibility namespaces that should also be recognized (but not generated).
     *
     * @return the namespaces
     */
    String[] compatibilityNamespaces() default {};

    enum Kind {
        SYSTEM,
        EXTENSION,
    }
}
