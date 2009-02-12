/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   10.12.2005 (Christoph Sieb): created
 */
package org.knime.workbench.editor2.actions.delegates;

import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.actions.AbstractNodeAction;
import org.knime.workbench.editor2.actions.ExecuteAndOpenViewAction;

/**
 * Editor action for "execute and open first view".
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class ExecuteAndOpenViewEditorAction extends AbstractEditorAction {
    /**
     * @see 
     * org.knime.workbench.editor2.actions.delegates.AbstractEditorAction
     *      #createAction(org.knime.workbench.editor2.WorkflowEditor)
     */
    @Override
    protected AbstractNodeAction createAction(final WorkflowEditor editor) {
        return new ExecuteAndOpenViewAction(editor);
    }
}
