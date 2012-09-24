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

import java.util.Locale;
import org.jboss.mgmt.AttributeListener;
import org.jboss.mgmt.AttributeValidator;
import org.jboss.mgmt.NestedBuilder;
import org.jboss.mgmt.annotation.Access;
import org.jboss.mgmt.annotation.RuntimeMode;
import org.jboss.mgmt.annotation.xml.XmlRender;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface AttributeBuilder<P> extends NestedBuilder<P> {

    AttributeBuilder<P> type(TypeMirror type);

    AttributeBuilder<P> description(Locale locale, String description);

    AttributeBuilder<P> access(Access access);

    AttributeBuilder<P> required(boolean required);

    AttributeBuilder<P> virtual(DeclaredType virtual); // extends VirtualAttribute

    AttributeBuilder<P> defaultValue(String value);

    AttributeBuilder<P> validator(DeclaredType validator);

    AttributeBuilder<P> validator(Class<? extends AttributeValidator> validator);

    AttributeBuilder<P> listener(DeclaredType listener, RuntimeMode... modes);

    AttributeBuilder<P> listener(Class<? extends AttributeListener> listener, RuntimeMode... modes);

    AttributeBuilder<P> version(String string);

    AttributeBuilder<P> xmlRenderAs(XmlRender.As as);

    AttributeBuilder<P> xmlName(String string);

    ReferenceBuilder<AttributeBuilder<P>> reference();

    P end();
}
