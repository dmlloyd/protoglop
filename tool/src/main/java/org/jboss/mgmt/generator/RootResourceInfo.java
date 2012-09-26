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

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class RootResourceInfo {
    private final String name;
    private final String type;
    private final String[] provides;
    private final String xmlName;
    private final ResourceInfo resourceInfo;

    RootResourceInfo(final String name, final String type, final String[] provides, final String xmlName, final ResourceInfo resourceInfo) {
        this.name = name;
        this.type = type;
        this.provides = provides;
        this.xmlName = xmlName;
        this.resourceInfo = resourceInfo;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String[] getProvides() {
        return provides;
    }

    public String getXmlName() {
        return xmlName;
    }

    public ResourceInfo getResourceInfo() {
        return resourceInfo;
    }
}
