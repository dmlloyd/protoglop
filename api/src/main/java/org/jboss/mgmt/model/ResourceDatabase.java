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

package org.jboss.mgmt.model;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

/**
 * Non-thread-safe fast {@code int}-keyed set of resource nodes.  Designed for efficient copy-on-write usage.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class ResourceDatabase extends AbstractCollection<ResourceNode> implements Cloneable, Collection<ResourceNode> {
    private ResourceNode[][] table;
    private int size;
    private int threshold;

    private static final double LOAD_FACTOR = 0.6;

    public ResourceDatabase() {
        size = 0;
        table = new ResourceNode[16][];
        threshold = (int) (table.length * LOAD_FACTOR);
    }

    public ResourceDatabase(ResourceDatabase orig) {
        table = orig.table.clone();
        size = orig.size;
        threshold = orig.threshold;
    }

    public ResourceDatabase clone() {
        try {
            return (ResourceDatabase) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    public Iterator<ResourceNode> iterator() {
        return null;
    }

    public int size() {
        return size;
    }
}
