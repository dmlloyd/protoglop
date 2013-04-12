/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
import org.jboss.mgmt.Resource;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * A named reference to an external resource.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@Retention(SOURCE)
@Target(METHOD)
public @interface Reference {

    /**
     * The name of the scope to refer to.  An element which encloses this one must have such a scope defined.
     * By convention, scope names should be plural, and qualified by the namespace of their defining schema.
     *
     * @return the scope name
     */
    String scopeName();

    /**
     * The type of resource being referred to.
     *
     * @return the type of resource
     */
    Class<? extends Resource> resourceType() default Resource.class;

    /**
     * Trigger a refresh of the attribute if the referenced resource changes.
     *
     * @return the monitor setting
     */
    boolean monitor() default false;
}
