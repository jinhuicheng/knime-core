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
 *   11.01.2007 (sieb): created
 */
package org.knime.workbench.editor2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.swing.UIManager;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.UnsupportedWorkflowVersionException;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.node.workflow.WorkflowCreationHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryType;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.util.LockFailedException;
import org.knime.workbench.editor2.actions.CheckUpdateMetaNodeLinkAllAction;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * A runnable which is used by the {@link WorkflowEditor} to load a workflow with a progress bar. NOTE: As the
 * {@link UIManager} holds a reference to this runnable an own class file is necessary such that all references to the
 * created workflow manager can be deleted, otherwise the manager cannot be deleted later and the memory cannot be
 * freed.
 *
 * @author Christoph Sieb, University of Konstanz
 * @author Fabian Dill, University of Konstanz
 */
class LoadWorkflowRunnable extends PersistWorkflowRunnable {

    /** Message returned by {@link #getLoadingCanceledMessage()} in case the loading has been canceled
     * due to a (future) version conflict. (See also AP-7982)
     */
    static final String INCOMPATIBLE_VERSION_MSG = "Canceled workflow load due to incompatible version";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(LoadWorkflowRunnable.class);

    private WorkflowEditor m_editor;

    private File m_workflowFile;

    private File m_mountpointRoot;

    private URI m_mountpointURI;

    private Throwable m_throwable = null;

    /** Message, which is non-null if the user canceled to the load. */
    private String m_loadingCanceledMessage;

    /**
     * Creates a new runnable that load a workflow.
     *
     * @param editor the {@link WorkflowEditor} for which the workflow should be loaded
     * @param uri the URI from the explorer
     * @param workflowFile the workflow file from which the workflow should be loaded (or created = empty workflow file)
     * @param mountpointRoot the root directory of the mountpoint in which the workflow is contained
     */
    public LoadWorkflowRunnable(final WorkflowEditor editor, final URI uri, final File workflowFile,
        final File mountpointRoot) {
        m_editor = editor;
        m_mountpointURI = uri;
        m_workflowFile = workflowFile;
        m_mountpointRoot = mountpointRoot;
    }

    /**
     *
     * @return the throwable which was thrown during the loading of the workflow or null, if no throwable was thrown
     */
    Throwable getThrowable() {
        return m_throwable;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void run(final IProgressMonitor pm) {
        // indicates whether to create an empty workflow
        // this is done if the file is empty
        boolean createEmptyWorkflow = false;

        // name of workflow will be null (uses directory name then)
        String name = null;

        m_throwable = null;

        try {
            // create progress monitor
            ProgressHandler progressHandler = new ProgressHandler(pm, 101, "Loading workflow...");
            final CheckCancelNodeProgressMonitor progressMonitor = new CheckCancelNodeProgressMonitor(pm);
            progressMonitor.addProgressListener(progressHandler);

            File workflowDirectory = m_workflowFile.getParentFile();
            Display d = Display.getDefault();
            GUIWorkflowLoadHelper loadHelper = new GUIWorkflowLoadHelper(d, workflowDirectory.getName(),
                m_mountpointURI, workflowDirectory, m_mountpointRoot);
            final WorkflowLoadResult result =
                WorkflowManager.loadProject(workflowDirectory, new ExecutionMonitor(progressMonitor), loadHelper);
            final WorkflowManager wm = result.getWorkflowManager();
            m_editor.setWorkflowManager(wm);
            pm.subTask("Finished.");
            pm.done();
            if (wm.isDirty()) {
                m_editor.markDirty();
            }

            final IStatus status = createStatus(result, !result.getGUIMustReportDataLoadErrors());
            String message;
            switch (status.getSeverity()) {
                case IStatus.OK:
                    message = "No problems during load.";
                    break;
                case IStatus.WARNING:
                    message = "Warnings during load";
                    logPreseveLineBreaks(
                        "Warnings during load: " + result.getFilteredError("", LoadResultEntryType.Warning), false);
                    break;
                default:
                    message = "Errors during load";
                    logPreseveLineBreaks(
                        "Errors during load: " + result.getFilteredError("", LoadResultEntryType.Warning), true);
            }
            if (!status.isOK()) {
                showLoadErrorDialog(result, status, message);
            }
            final List<NodeID> linkedMNs = wm.getLinkedMetaNodes(true);
            if (!linkedMNs.isEmpty()) {
                final WorkflowEditor editor = m_editor;
                m_editor.addAfterOpenRunnable(new Runnable() {
                    @Override
                    public void run() {
                        postLoadCheckForMetaNodeUpdates(editor, wm, linkedMNs);
                    }
                });
            }
        } catch (FileNotFoundException fnfe) {
            m_throwable = fnfe;
            LOGGER.fatal("File not found", fnfe);
        } catch (IOException ioe) {
            m_throwable = ioe;
            if (m_workflowFile.length() == 0) {
                LOGGER.info("New workflow created.");
                // this is the only place to set this flag to true: we have an
                // empty workflow file, i.e. a new project was created
                // bugfix 1555: if an exception is thrown DO NOT create empty
                // workflow
                createEmptyWorkflow = true;
            } else {
                LOGGER.error("Could not load workflow from: " + m_workflowFile.getName(), ioe);
            }
        } catch (InvalidSettingsException ise) {
            LOGGER.error("Could not load workflow from: " + m_workflowFile.getName(), ise);
            m_throwable = ise;
        } catch (UnsupportedWorkflowVersionException uve) {
            m_loadingCanceledMessage = INCOMPATIBLE_VERSION_MSG;
            LOGGER.info(m_loadingCanceledMessage, uve);
            m_editor.setWorkflowManager(null);
        } catch (CanceledExecutionException cee) {
            m_loadingCanceledMessage = "Canceled loading workflow: " + m_workflowFile.getParentFile().getName();
            LOGGER.info(m_loadingCanceledMessage, cee);
            m_editor.setWorkflowManager(null);
        } catch (LockFailedException lfe) {
            StringBuilder error = new StringBuilder();
            error.append("Unable to load workflow \"");
            error.append(m_workflowFile.getParentFile().getName());
            if (m_workflowFile.getParentFile().exists()) {
                error.append("\"\nIt is in use by another user/instance.");
            } else {
                error.append("\"\nLocation does not exist.");
            }
            m_loadingCanceledMessage = error.toString();
            LOGGER.info(m_loadingCanceledMessage, lfe);
            m_editor.setWorkflowManager(null);
        } catch (Throwable e) {
            m_throwable = e;
            LOGGER.error("Workflow could not be loaded. " + e.getMessage(), e);
            m_editor.setWorkflowManager(null);
        } finally {
            // create empty WFM if a new workflow is created
            // (empty workflow file)
            if (createEmptyWorkflow) {
                WorkflowCreationHelper creationHelper = new WorkflowCreationHelper();
                WorkflowContext.Factory fac = new WorkflowContext.Factory(m_workflowFile.getParentFile());
                fac.setMountpointRoot(m_mountpointRoot);
                fac.setMountpointURI(m_mountpointURI);
                creationHelper.setWorkflowContext(fac.createContext());

                m_editor.setWorkflowManager(WorkflowManager.ROOT.createAndAddProject(name, creationHelper));
                // save empty project immediately
                // bugfix 1341 -> see WorkflowEditor line 1294
                // (resource delta visitor movedTo)
                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        m_editor.doSave(new NullProgressMonitor());
                    }
                });
                m_editor.setIsDirty(false);

            }
            // IMPORTANT: Remove the reference to the file and the
            // editor!!! Otherwise the memory cannot be freed later
            m_editor = null;
            m_workflowFile = null;
            m_mountpointRoot = null;
        }
    }

    private void showLoadErrorDialog(final WorkflowLoadResult result, final IStatus status, final String message) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                Shell shell = Display.getCurrent().getActiveShell();
                if (result.getMissingNodes().isEmpty()) {
                    // will not open if status is OK.
                    ErrorDialog.openError(shell, "Workflow Load", message, status);
                } else {
                    String missing = result.getMissingNodes().stream().map(i -> i.getNodeNameNotNull()).distinct()
                        .collect(Collectors.joining(", "));

                    String[] dialogButtonLabels = {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL};
                    MessageDialog dialog = new MessageDialog(shell, "Workflow contains missing nodes", null,
                        message + " due to missing nodes (" + missing
                            + "). Do you want to search and install the required extensions?",
                        MessageDialog.WARNING, dialogButtonLabels, 0);
                    if (dialog.open() == 0) {
                        Job j = new InstallMissingNodesJob(result.getMissingNodes());
                        j.setUser(true);
                        j.schedule();
                    }
                }
            }
        });
    }

    /** @return True if the load process has been interrupted. */
    public boolean hasLoadingBeenCanceled() {
        return m_loadingCanceledMessage != null;
    }

    /**
     * @return the loadingCanceledMessage, non-null if {@link #hasLoadingBeenCanceled()}.
     */
    public String getLoadingCanceledMessage() {
        return m_loadingCanceledMessage;
    }

    static void postLoadCheckForMetaNodeUpdates(final WorkflowEditor editor, final WorkflowManager parent,
        final List<NodeID> links) {
        StringBuilder m = new StringBuilder("The workflow contains ");
        if (links.size() == 1) {
            m.append("one metanode link (\"");
            m.append(parent.findNodeContainer(links.get(0)).getNameWithID());
            m.append("\").");
        } else {
            m.append(links.size()).append(" metanode links.");
        }
        m.append("\n\n").append("Do you want to check for updates now?");

        final String message = m.toString();
        final AtomicBoolean result = new AtomicBoolean(false);
        final IPreferenceStore corePrefStore = KNIMEUIPlugin.getDefault().getPreferenceStore();
        final String pKey = PreferenceConstants.P_META_NODE_LINK_UPDATE_ON_LOAD;
        String pref = corePrefStore.getString(pKey);
        boolean showInfoMsg = true;
        if (MessageDialogWithToggle.ALWAYS.equals(pref)) {
            result.set(true);
            showInfoMsg = false;
        } else if (MessageDialogWithToggle.NEVER.equals(pref)) {
            result.set(false);
        } else {
            final Display display = Display.getDefault();
            display.syncExec(new Runnable() {
                @Override
                public void run() {
                    Shell activeShell = display.getActiveShell();
                    MessageDialogWithToggle dlg =
                        MessageDialogWithToggle.openYesNoCancelQuestion(activeShell, "Metanode Link Update", message,
                            "Remember my decision", false, corePrefStore, pKey);
                    switch (dlg.getReturnCode()) {
                        case IDialogConstants.YES_ID:
                            result.set(true);
                            break;
                        default:
                            result.set(false);
                    }
                }
            });
        }
        if (result.get()) {
            new CheckUpdateMetaNodeLinkAllAction(editor, showInfoMsg).run();
        }
    }

}
