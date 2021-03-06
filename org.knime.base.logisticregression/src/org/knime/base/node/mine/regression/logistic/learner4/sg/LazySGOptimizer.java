/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   28.03.2017 (Adrian): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.sg;

import java.util.Arrays;

import org.knime.base.node.mine.regression.logistic.learner4.data.TrainingData;
import org.knime.base.node.mine.regression.logistic.learner4.data.TrainingRow;
import org.knime.base.node.mine.regression.logistic.learner4.data.TrainingRow.FeatureIterator;

/**
 * Lazy implementation of stochastic gradient descent like optimization schemes.
 *
 * @author Adrian Nembach, KNIME.com
 */
final class LazySGOptimizer <T extends TrainingRow, U extends LazyUpdater<T>, R extends LazyRegularizationUpdater> extends AbstractSGOptimizer<T, U, R> {

    private int[] m_lastVisited;


    /**
     * Creates a LazySGOptimizer.
     * All arguments are delegated to the super class.
     *
     * @param data the training data to learn on
     * @param loss the loss to minimize
     * @param updaterFactory factory for a LazyUpdater
     * @param regularizationUpdater updater for regularization term
     * @param learningRateStrategy the strategy used for the learning rate for example fixed
     * @param stoppingCriterion determines when to stop the training
     * @param calcCovMatrix flag that indicates whether the covariance matrix of the coefficients should be calculated
     */
    public LazySGOptimizer(final TrainingData<T> data, final Loss<T> loss, final UpdaterFactory<T, U> updaterFactory,
        final R regularizationUpdater, final LearningRateStrategy<T> learningRateStrategy,
        final StoppingCriterion<T> stoppingCriterion, final boolean calcCovMatrix) {
        super(data, loss, updaterFactory, regularizationUpdater, learningRateStrategy, stoppingCriterion, calcCovMatrix);
        m_lastVisited = new int[data.getFeatureCount()];
    }



    /**
     * {@inheritDoc}
     */
    @Override
    protected void performUpdate(final T x, final U updater, final double[] gradient,
        final WeightMatrix<T> beta, final double stepSize, final int iteration) {
        updater.update(x, gradient, beta, stepSize, iteration);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void prepareIteration(final WeightMatrix<T> beta, final T x, final U updater, final R regUpdater,
        final int iteration) {
        // apply lazy updates
        updater.lazyUpdate(beta, x, m_lastVisited, iteration);
        regUpdater.lazyUpdate(beta, x, m_lastVisited, iteration);
        // update when present features were last visited
        for (FeatureIterator iter = x.getFeatureIterator(); iter.next();) {
            m_lastVisited[iter.getFeatureIndex()] = iteration;
        }
    }



    /**
     * {@inheritDoc}
     */
    @Override
    protected void postProcessEpoch(final WeightMatrix<T> beta, final U updater, final R regUpdater) {
        updater.resetJITSystem(beta, m_lastVisited);
        regUpdater.resetJITSystem(beta, m_lastVisited);
        Arrays.fill(m_lastVisited, 0);
    }



    /**
     * {@inheritDoc}
     */
    @Override
    protected void normalize(final WeightMatrix<T> beta, final U updater, final int iteration) {
        updater.normalize(beta, m_lastVisited, iteration);
        for (int i = 0; i < m_lastVisited.length; i++) {
            m_lastVisited[i] = iteration + 1;
        }
    }

}
