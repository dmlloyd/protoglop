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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jboss.mgmt.AttributeListener;
import org.jboss.mgmt.AttributeValidator;
import org.jboss.mgmt.annotation.Access;
import org.jboss.mgmt.annotation.RuntimeMode;
import org.jboss.mgmt.annotation.xml.XmlRender;

import javax.lang.model.type.DeclaredType;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class AttributeBuilderImpl<P extends GeneralResourceBuilderImpl<P>> implements AttributeBuilder<P> {
    private final P parent;

    private String name;
    private DeclaredType type;
    private Access access = Access.READ_WRITE;
    private boolean required = true;
    private DeclaredType virtual;
    private String value;
    private final List<DeclaredType> validators = new ArrayList<DeclaredType>();
    private String version;
    private String defaultValue;
    private XmlRender.As renderAs = XmlRender.As.ELEMENT;
    private String xmlName;

    AttributeBuilderImpl(final P parent) {
        this.parent = parent;
    }

    public AttributeBuilder<P> name(final String name) {
        this.name = name;
        return this;
    }

    public AttributeBuilder<P> type(final DeclaredType type) {
        this.type = type;
        return this;
    }

    public AttributeBuilder<P> description(final Locale locale, final String description) {
        // todo
        return this;
    }

    public AttributeBuilder<P> access(final Access access) {
        if (access != null) this.access = access;
        return this;
    }

    public AttributeBuilder<P> required(final boolean required) {
        this.required = required;
        return this;
    }

    public AttributeBuilder<P> virtual(final DeclaredType virtual) {
        this.virtual = virtual;
        return this;
    }

    public AttributeBuilder<P> defaultValue(final String value) {
        defaultValue = value;
        return this;
    }

    public AttributeBuilder<P> validator(final DeclaredType validator) {
        validators.add(validator);
        return this;
    }

    public AttributeBuilder<P> validator(final Class<? extends AttributeValidator> validator) {
        // todo
        return this;
    }

    public AttributeBuilder<P> listener(final DeclaredType listener, final RuntimeMode... modes) {
        // todo
        return this;
    }

    public AttributeBuilder<P> listener(final Class<? extends AttributeListener> listener, final RuntimeMode... modes) {
        // todo
        return this;
    }

    public AttributeBuilder<P> version(final String version) {
        this.version = version;
        return this;
    }

    public AttributeBuilder<P> xmlRenderAs(final XmlRender.As renderAs) {
        if (renderAs != null) this.renderAs = renderAs;
        return this;
    }

    public AttributeBuilder<P> xmlName(final String xmlName) {
        this.xmlName = xmlName;
        return this;
    }

    public ReferenceBuilder<AttributeBuilder<P>> reference() {
        // todo
        return null;
    }

    public P end() {
        return parent;
    }

    String getName() {
        return name;
    }

    DeclaredType getType() {
        return type;
    }

    Access getAccess() {
        return access;
    }

    boolean isRequired() {
        return required;
    }

    DeclaredType getVirtual() {
        return virtual;
    }

    String getValue() {
        return value;
    }

    List<DeclaredType> getValidators() {
        return validators;
    }

    String getVersion() {
        return version;
    }

    String getDefaultValue() {
        return defaultValue;
    }

    String getRootDescription() {
        // todo - Locale.ROOT description value
        return "attribute value";
    }

    XmlRender.As getXmlRenderAs() {
        return renderAs;
    }

    String getXmlName() {
        return xmlName;
    }
}
