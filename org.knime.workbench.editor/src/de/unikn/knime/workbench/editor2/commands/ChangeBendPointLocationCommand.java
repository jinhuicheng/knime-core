/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   02.03.2006 (Christoph Sieb): created
 */
package de.unikn.knime.workbench.editor2.commands;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.ZoomManager;
import org.knime.core.node.workflow.ConnectionContainer;

import de.unikn.knime.workbench.editor2.WorkflowEditor;
import de.unikn.knime.workbench.editor2.extrainfo.ModellingConnectionExtraInfo;

/**
 * GEF Command for changing the location of a <code>ConnectionContainer</code>
 * in the workflow. The bounds are stored into the <code>ExtraInfo</code>
 * object of the <code>ConnectionContainer</code>
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class ChangeBendPointLocationCommand extends Command {
    private Point m_locationShift;

    private ConnectionContainer m_container;

    private ModellingConnectionExtraInfo m_extraInfo;

    private ZoomManager m_zoomManager;

    /**
     * @param container The node container to change
     * @param locationShift the values (x,y) to change the location of all
     *            bendpoints
     */
    public ChangeBendPointLocationCommand(final ConnectionContainer container,
            final Point locationShift, final ZoomManager zoomManager) {
        if (container == null
                || container.getExtraInfo() == null
                || !(container.getExtraInfo() instanceof ModellingConnectionExtraInfo)) {
            return;
        }

        m_extraInfo = (ModellingConnectionExtraInfo)container.getExtraInfo();
        m_locationShift = locationShift;
        m_container = container;

        m_zoomManager = zoomManager;
    }

    /**
     * Shift all bendpoints in positive shift direction.
     * 
     * @see org.eclipse.gef.commands.Command#execute()
     */
    @Override
    public void execute() {
        changeBendpointsExtraInfo(false);
    }

    /**
     * Shift all bendpoints in negative shift direction.
     * 
     * @see org.eclipse.gef.commands.Command#undo()
     */
    @Override
    public void undo() {
        changeBendpointsExtraInfo(true);
    }

    private void changeBendpointsExtraInfo(final boolean shiftBack) {
        if (m_extraInfo == null) {
            return;
        }

        int[][] bendpoints = m_extraInfo.getAllBendpoints();

        Point locationShift = m_locationShift.getCopy();

        WorkflowEditor.adaptZoom(m_zoomManager, locationShift, false);

        int length = bendpoints.length;
        int shiftX = shiftBack ? locationShift.x * -1 : locationShift.x;
        int shiftY = shiftBack ? locationShift.y * -1 : locationShift.y;

        for (int i = 0; i < length; i++) {

            // get old
            int x = m_extraInfo.getBendpoint(i)[0];
            int y = m_extraInfo.getBendpoint(i)[1];

            // remove the old point
            m_extraInfo.removeBendpoint(i);

            // set the new point
            m_extraInfo.addBendpoint(x + shiftX, y + shiftY, i);
        }

        // must set explicitly so that event is fired by container
        m_container.setExtraInfo(m_extraInfo);
    }
}
