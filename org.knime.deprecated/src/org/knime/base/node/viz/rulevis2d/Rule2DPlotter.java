/*
 * ------------------------------------------------------------------
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
 *   23.05.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.rulevis2d;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.knime.base.node.util.DataArray;
import org.knime.base.node.util.DefaultDataArray;
import org.knime.base.node.viz.scatterplot.ScatterPlotter;
import org.knime.base.node.viz.scatterplot.ScatterProps;
import org.knime.base.util.coordinate.Coordinate;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.FuzzyIntervalValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.FuzzyIntervalCell;
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * Passes the normalized rules to a Rule2DDrawingPane.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class Rule2DPlotter extends ScatterPlotter {

    /** Menu entry and action command for hiliting the rules. */
    public static final String HILITE_RULES = "HiLite selected rules";

    /** Menu entry and action command for unhiliting the rules. */
    public static final String UNHILITE_RULES = "UnHiLite selected rules";

    /** Menu entry and action command for hiding unhilited rules. */
    public static final String HIDE_UNHILITED_RULES = "Hide unHiLited rules";

    /** Menu entry and action command to clear hilited rules . */
    public static final String CLEAR_RULE_HILITE = "Clear rule hiLite";

    private DataArray m_rules;

    private JMenuItem m_hiliteRules;

    private JMenuItem m_unhiliteRules;

    // private DataArray m_data;

    /**
     * Creates an instance of the Rule2DPlotter.
     * 
     * @param data - the data points
     * @param rules - the rules
     * @param props - the scatter properties.
     * @param initialWidth - the initial width of the panel.
     */
    public Rule2DPlotter(final DataArray data, final DataArray rules,
            final ScatterProps props, final int initialWidth) {
        super(data, initialWidth, props, new Rule2DDrawingPane());
        m_rules = rules;
        getDrawingPane().addMouseListener(new MouseAdapter() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void mouseReleased(final MouseEvent arg0) {
                if (m_unhiliteRules != null && m_hiliteRules != null) {
                    if (getDrawingPane().getNrSelectedRules() > 0) {
                        m_hiliteRules.setEnabled(true);
                        m_unhiliteRules.setEnabled(true);
                    } else {
                        m_hiliteRules.setEnabled(false);
                        m_unhiliteRules.setEnabled(false);
                    }
                }
            }

        });
    }

    /**
     * Sets the rules.
     * 
     * @param rules - the rules.
     */
    public void setRules(final DataArray rules) {
        m_rules = rules;
    }

    /**
     * Sets the hiliteHandler for the rules to the drawingPane.
     * 
     * @param handlr - hilitehandler for the rules.
     */
    public void setRuleHiLiteHandler(final HiLiteHandler handlr) {
        getDrawingPane().setHiLiteHandler(handlr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JMenu getHiLiteMenu() {
        JMenu menu = super.getHiLiteMenu();
        if (m_hiliteRules == null && m_unhiliteRules == null) {
            menu.addSeparator();
            JMenuItem item;
            /*-------"hilite selected rules"----*/
            m_hiliteRules = new JMenuItem(HILITE_RULES);
            m_hiliteRules.setEnabled(false);
            m_hiliteRules.addActionListener(this);
            menu.add(m_hiliteRules);
            /*----------"unhilite selected rules"-----*/
            m_unhiliteRules = new JMenuItem(UNHILITE_RULES);
            // item.setEnabled(rulesSelected);
            m_unhiliteRules.addActionListener(this);
            m_unhiliteRules.setEnabled(false);
            menu.add(m_unhiliteRules);
            /* --- "[ ] hide unhilited rules" --- */
            item = new JCheckBoxMenuItem(HIDE_UNHILITED_RULES, getDrawingPane()
                    .isHideUnhilitedRules());
            item.addActionListener(this);
            menu.add(item);
        }
        return menu;
    }

    /*------------action listener--------------*/

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        super.actionPerformed(e);
        if (e.getActionCommand().equals(HILITE_RULES)) {
            // hilite selected rules with the rules hilite handler
            getDrawingPane().hiliteSelectedRules();
        } else if (e.getActionCommand().equals(UNHILITE_RULES)) {
            // unhilte selected rules with the rules hilite handler which is in
            // the FuzzyRuleDrawingPane
            getDrawingPane().unhiliteSelectedRules();
        } else if (e.getActionCommand().equals(HIDE_UNHILITED_RULES)) {
            // fade unhilited rules by setting a flag in the
            getDrawingPane().setHideUnhilitedRules(
                    !getDrawingPane().isHideUnhilitedRules());
        }
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void fillPopupMenu(final JPopupMenu menu) {
        super.fillPopupMenu(menu);
        menu.addSeparator();
        JMenuItem item;
        /*-------"hilite selected rules"----*/
        m_hiliteRules = new JMenuItem(HILITE_RULES);
        m_hiliteRules.setEnabled(false);
        m_hiliteRules.addActionListener(this);
        menu.add(m_hiliteRules);
        /*----------"unhilite selected rules"-----*/
        m_unhiliteRules = new JMenuItem(UNHILITE_RULES);
        // item.setEnabled(rulesSelected);
        m_unhiliteRules.addActionListener(this);
        m_unhiliteRules.setEnabled(false);
        menu.add(m_unhiliteRules);
        /* --- "[ ] hide unhilited rules" --- */

        item = new JCheckBoxMenuItem(HIDE_UNHILITED_RULES, getDrawingPane()
                .isHideUnhilitedRules()
                && areRulesHilited());
        item.addActionListener(this);
        menu.add(item);
    }

    private boolean areRulesHilited() {
        boolean areRulesHilited = false;
        if (getDrawingPane().getHiLiteHandler() != null) {
            areRulesHilited = getDrawingPane().getHiLiteHandler()
                    .getHiLitKeys().size() > 0;
        }
        return areRulesHilited;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updatePaintModel() {
        super.updatePaintModel();

        if (m_rules != null) {
            Rule2DDrawingPane drawingPane = getDrawingPane();
            drawingPane.setOriginalRuleTable(m_rules);

            String xName = getXColName();
            String yName = getYColName();
            int xIdx = -1;
            int yIdx = -1;
            if (xName != null && yName != null) {
                xIdx = m_rules.getDataTableSpec().findColumnIndex(xName);
                yIdx = m_rules.getDataTableSpec().findColumnIndex(yName);
            }

            if (xIdx >= 0 && yIdx >= 0) {
                Coordinate x = getColHeader()
                        .getCoordinate();
                Coordinate y = getRowHeader()
                        .getCoordinate();

                // check if the coordinates are valid
                if (x == null || y == null) {
                    return;
                }

                // calculate the coordinates of the rules here
                // List<DataRow> rows = new ArrayList<DataRow>();
                DataColumnSpecCreator creator = new DataColumnSpecCreator(
                        "xValues", FuzzyIntervalCell.TYPE);
                DataColumnSpec col1 = creator.createSpec();
                creator = new DataColumnSpecCreator("yValues",
                        FuzzyIntervalCell.TYPE);
                DataColumnSpec col2 = creator.createSpec();
                DataTableSpec spec = new DataTableSpec(new DataColumnSpec[]{
                        col1, col2});
                DataContainer rows = new DataContainer(spec);
                for (RowIterator itr = m_rules.iterator(); itr.hasNext();) {
                    DataRow currRow = itr.next();
                    DataCell[] newCells = new DataCell[2];

                    for (int cell = 0; cell < currRow.getNumCells(); cell++) {
//                        if (!m_rules.getDataTableSpec().getColumnSpec(cell)
//                                .getType().isCompatible(
//                                        FuzzyIntervalValue.class)) {
//                            continue;
//                        }
                        Rectangle rect = calculateDrawingRectangle();
                        double a;
                        double b;
                        double c;
                        double d;
                        if (cell == xIdx) {
                            if (currRow.getCell(cell).isMissing()) {
                                // normalize xValues
                                a = getXmin();
                                b = getXmin();
                                c = getXmax();
                                d = getXmax();

                            } else {
                                // normalize xValues
                                a = ((FuzzyIntervalValue)currRow.getCell(cell))
                                        .getMinSupport();
                                b = ((FuzzyIntervalValue)currRow.getCell(cell))
                                        .getMinCore();
                                c = ((FuzzyIntervalValue)currRow.getCell(cell))
                                        .getMaxCore();
                                d = ((FuzzyIntervalValue)currRow.getCell(cell))
                                        .getMaxSupport();
                            }

                            double newA = x.calculateMappedValue(
                                    new DoubleCell(a), rect.width, true);
                            double newB = x.calculateMappedValue(
                                    new DoubleCell(b), rect.width, true);
                            double newC = x.calculateMappedValue(
                                    new DoubleCell(c), rect.width, true);
                            double newD = x.calculateMappedValue(
                                    new DoubleCell(d), rect.width, true);
                            DataCell newInterval = new FuzzyIntervalCell(rect.x
                                    + newA, rect.x + newB, rect.x + newC,
                                    rect.x + newD);
                            newCells[0] = newInterval;
                        }
                        if (cell == yIdx) {
                            if (currRow.getCell(cell).isMissing()) {
                                a = getYmin();
                                b = getYmin();
                                c = getYmax();
                                d = getYmax();
                            } else {
                                // normalize yValues
                                a = ((FuzzyIntervalValue)currRow.getCell(cell))
                                        .getMinSupport();
                                b = ((FuzzyIntervalValue)currRow.getCell(cell))
                                        .getMinCore();
                                c = ((FuzzyIntervalValue)currRow.getCell(cell))
                                        .getMaxCore();
                                d = ((FuzzyIntervalValue)currRow.getCell(cell))
                                        .getMaxSupport();
                            }
                            double newA = y.calculateMappedValue(
                                    new DoubleCell(a), rect.height, true);
                            double newB = y.calculateMappedValue(
                                    new DoubleCell(b), rect.height, true);
                            double newC = y.calculateMappedValue(
                                    new DoubleCell(c), rect.height, true);
                            double newD = y.calculateMappedValue(
                                    new DoubleCell(d), rect.height, true);
                            DataCell newInterval = new FuzzyIntervalCell(rect.y
                                    + rect.height - newD, rect.y + rect.height
                                    - newC, rect.y + rect.height - newB, rect.y
                                    + rect.height - newA);
                            newCells[1] = newInterval;
                        }
                    }
                    // create new row out of the normalized cells
                    rows.addRowToTable(new DefaultRow(currRow.getKey(),
                            newCells));

                }
                rows.close();
                drawingPane.setNormalizedRules(new DefaultDataArray(rows
                        .getTable(), 1, m_rules.size()));
            }
            super.updatePaintModel();
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Rule2DDrawingPane getDrawingPane() {
        return (Rule2DDrawingPane)super.getDrawingPane();
    }

}
