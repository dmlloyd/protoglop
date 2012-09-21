/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * A model root.  Model roots may be used in the model in places which accept any resource of a type.
 */
@Target(TYPE)
@Retention(SOURCE)
public @interface RootResource {

    /**
     * The type of the root.
     *
     * @return the type
     */
    String type();

    /**
     * The name of the root in the model.
     *
     * @return the name
     */
    String name() default "";

    /**
     * The model version.
     *
     * @return the model version
     */
    String version();

    /**
     * The kind of root.  This affects generation of the XML namespace.
     *
     * @return the kind of root
     */
    Kind kind() default Kind.EXTENSION;

    /**
     * The base namespace, e.g. "core.logging".  Namespaces starting with "core" and "jboss" are reserved
     * for use by JBoss.
     *
     * @return the namespace
     */
    String namespace();

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
