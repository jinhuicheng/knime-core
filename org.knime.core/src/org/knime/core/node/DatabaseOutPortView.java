/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   14.02.2008 (gabriel): created
 */
package org.knime.core.node;

import org.knime.core.data.DataTableSpec;

/**
 * Database out port overrides the data put port view to visualize the
 * <code>DataTableSpec</code> after configure and the 
 * <code>BufferedDataTable</code> after execute.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class DatabaseOutPortView extends DataOutPortView {

    /**
     * Create new database outport view.
     * 
     * @param nodeName the name of the underlying node
     * @param portName the name of the port to display view on
     */
    DatabaseOutPortView(final String nodeName, final String portName) {
        super(nodeName, portName);    }
    
    
    /**
     * Override this method to unpack internal <code>BufferedDataTable</code>
     * and <code>DataTableSpec</code>.
     * {@inheritDoc}
     */
    @Override
    void update(final PortObject portObj, final PortObjectSpec portSpec) {
        BufferedDataTable table = null;
        DataTableSpec spec = null;
        if (portObj != null) {
            table = ((DatabaseContent) portObj).getDataTable();
            spec = table.getDataTableSpec();
        } else {
            if (portSpec != null) {
                spec = ((DatabaseContentSpec) portSpec).getDataTableSpec();
            }
        }
        super.update(table, spec);
    }
    
}
