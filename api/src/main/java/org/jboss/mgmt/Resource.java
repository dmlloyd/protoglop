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

package org.jboss.mgmt;

/**
 * Base interface type for all resources.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface Resource {

    /**
     * Get the stored comment string for this resource.  The comment is persisted before the start
     * of the resource's element.
     *
     * @return the comment text
     */
    String getPreComment();

    /**
     * Get the stored post-element string for this resource.  The comment is persisted before the close
     * of the resource's element.
     *
     * @return the comment text
     */
    String getPostComment();

    /**
     * Get the name of this resource.
     *
     * @return the name of the resource, or {@code null} if there is no name
     */
    String getName();

    /**
     * Get the parent of this resource, or {@code null} if it is a root resource.
     *
     * @return the parent, or {@code null}
     */
    Resource getParent();
}
