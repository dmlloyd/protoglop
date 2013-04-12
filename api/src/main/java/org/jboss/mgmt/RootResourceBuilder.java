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

import javax.xml.stream.XMLStreamReader;

/**
 * Infrastructure class used by generated root resource implementations.
 *
 * @param <R> the root resource type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class RootResourceBuilder<R extends Resource> {

    /**
     * Construct this resource node and all child nodes.
     *
     * @param parentNode the node under which to nest, or {@code null} for an ultimate root
     * @return the constructed node
     */
    protected abstract ResourceNode<R> construct(ResourceNode<?> parentNode);

    /**
     * Populate this builder instance from the given XML.
     *
     * @param reader the XML to read from
     * @throws org.jboss.mgmt.XMLParseException if the XML is not properly formed or is not valid
     */
    public abstract void fromXml(XMLStreamReader reader) throws XMLParseException;
}
