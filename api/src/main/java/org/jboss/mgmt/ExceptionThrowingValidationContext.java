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

import java.util.ArrayList;
import java.util.List;
import org.jboss.msc.txn.Problem;
import org.jboss.msc.txn.Transaction;
import org.jboss.msc.txn.ValidateContext;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ExceptionThrowingValidationContext implements ValidateContext {
    private final List<String> problems = new ArrayList<String>(0);

    public void reportProblem(final String problem) {
        problems.add(problem);
    }

    public void throwProblems() throws IllegalArgumentException {
        if (problems.isEmpty()) {
            return;
        }
        final StringBuilder msg = new StringBuilder("Validation failed for the following reason(s):");
        for (String problem : problems) {
            msg.append("\n    ").append(problem);
        }
        throw new IllegalArgumentException(msg.toString());
    }

    public boolean isCancelRequested() {
        return false;
    }

    public void cancelled() {
    }

    public void addProblem(final Problem reason) {
    }

    public void complete() {
    }

    public void begin() {
    }

    public void end() {
    }

    public Transaction getTransaction() {
        return null;
    }

    public void addProblem(final Problem.Severity severity, final String message) {
    }

    public void addProblem(final Problem.Severity severity, final String message, final Throwable cause) {
    }

    public void addProblem(final String message, final Throwable cause) {
    }

    public void addProblem(final String message) {
    }

    public void addProblem(final Throwable cause) {
    }
}
