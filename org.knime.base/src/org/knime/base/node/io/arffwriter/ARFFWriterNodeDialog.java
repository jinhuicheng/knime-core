/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 * History
 *   11.02.2005 (ohl): created
 */
package org.knime.base.node.io.arffwriter;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.FilesHistoryPanel;
import org.knime.core.node.util.FilesHistoryPanel.LocationValidation;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.util.FileUtil;

/**
 * Contains the dialog for the ARFF file writer.
 *
 * @author Peter Ohl, University of Konstanz
 */
public class ARFFWriterNodeDialog extends NodeDialogPane {

    private final FilesHistoryPanel m_filePanel;

    private final JRadioButton m_overwritePolicyAbortButton;

    private final JRadioButton m_overwritePolicyOverwriteButton;

    /**
     * Creates a new ARFF file reader dialog.
     */
    public ARFFWriterNodeDialog() {
        m_filePanel =
                new FilesHistoryPanel(createFlowVariableModel(ARFFWriterNodeModel.CFGKEY_FILENAME, FlowVariable.Type.STRING),
                    "org.knime.base.node.io.arffwriter", LocationValidation.FileOutput, ".arff");
        m_filePanel.setDialogTypeSaveWithExtension(".arff");
        m_filePanel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                String selFile = m_filePanel.getSelectedFile().trim();
                if (!selFile.isEmpty()) {
                    try {
                        URL newUrl = FileUtil.toURL(selFile);
                        Path path = FileUtil.resolveToPath(newUrl);
                        boolean isLocalDestination = path != null;
                        m_overwritePolicyAbortButton.setEnabled(isLocalDestination);
                        m_overwritePolicyOverwriteButton.setEnabled(isLocalDestination);
                    } catch (IOException | URISyntaxException | InvalidPathException ex) {
                        // ignore
                    }
                }
            }
        });

        m_overwritePolicyOverwriteButton = new JRadioButton("Overwrite");
        m_overwritePolicyAbortButton = new JRadioButton("Abort");
        ButtonGroup bg = new ButtonGroup();
        bg.add(m_overwritePolicyOverwriteButton);
        bg.add(m_overwritePolicyAbortButton);
        m_overwritePolicyAbortButton.doClick();

        addTab("Settings", initLayout());
    }

    private JPanel initLayout() {
        final JPanel filePanel = new JPanel();
        filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.X_AXIS));
        filePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Output location:"));
        filePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, m_filePanel.getPreferredSize().height));
        filePanel.add(m_filePanel);

        final JPanel optionsPanel = new JPanel(new GridBagLayout());
        optionsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
            .createEtchedBorder(), "Writer options:"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        optionsPanel.add(new JLabel(" If file exists... "), gbc);

        gbc.gridx += 1;
        gbc.insets = new Insets(5, 10, 5, 10);
        optionsPanel.add(m_overwritePolicyOverwriteButton, gbc);

        gbc.gridx += 1;
        optionsPanel.add(m_overwritePolicyAbortButton, gbc);

        //empty panel to eat up extra space
        gbc.gridx++;
        gbc.gridy++;
        gbc.weightx = 1;
        gbc.weighty = 1;
        optionsPanel.add(new JPanel(), gbc);

        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(filePanel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(optionsPanel);
        return panel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        settings.addString(ARFFWriterNodeModel.CFGKEY_FILENAME, m_filePanel.getSelectedFile().trim());

        settings.addBoolean(ARFFWriterNodeModel.CFGKEY_OVERWRITE_OK,
                m_overwritePolicyOverwriteButton.isSelected());

        m_filePanel.addToHistory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        // set the selected file
        m_filePanel.updateHistory();
        m_filePanel.setSelectedFile(settings.getString(ARFFWriterNodeModel.CFGKEY_FILENAME, ""));

        if (settings.getBoolean(ARFFWriterNodeModel.CFGKEY_OVERWRITE_OK, false)) {
            m_overwritePolicyOverwriteButton.doClick();
        } else {
            m_overwritePolicyAbortButton.doClick();
        }
    }
}
