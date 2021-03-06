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
 * ---------------------------------------------------------------------
 *
 * Created on 2013.08.09. by Gabor Bakos
 */
package org.knime.base.node.rules.engine.rsyntax;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.TokenMaker;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.knime.base.node.rules.engine.Rule;
import org.knime.base.node.rules.engine.Rule.BooleanConstants;
import org.knime.base.node.rules.engine.Rule.Operators;
import org.knime.base.node.rules.engine.RuleNodeSettings;

/**
 * The {@link RuleParser} for the {@link RSyntaxTextArea}.
 *
 * @since 2.9
 * @author Gabor Bakos
 */
public class RuleParser extends AbstractRuleParser {

    /**
     * Language support class for the rule language.
     *
     * @author Gabor Bakos
     * @since 2.8
     */
    public static class RuleLanguageSupport extends AbstractRuleParser.AbstractRuleLanguageSupport<AbstractRuleParser> {
        /**
         * Constructs {@link RuleLanguageSupport}.
         */
        public RuleLanguageSupport() {
            super(AbstractRuleParser.SYNTAX_STYLE_RULE, RuleLanguageSupport.class, KnimeTokenMaker.class);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected RuleParser createParser() {
            return new RuleParser(true, RuleNodeSettings.RuleEngine);
        }
    }

    /**
     * Wraps a {@link TokenMaker} and makes the {@link Operators} {@link Operators#toString()} a keyword.
     *
     * @author Gabor Bakos
     * @since 2.8
     */
    public static class KnimeTokenMaker extends AbstractRuleParser.WrappedTokenMaker {

        /**
         * Constructs a {@link Rule} token maker based on the Java {@link TokenMaker}.
         */
        public KnimeTokenMaker() {
            super(TokenMakerFactory.getDefaultInstance().getTokenMaker("text/java"), new AbstractRuleParser(true,
                RuleNodeSettings.RuleEngine).getOperators());
        }
    }

    /**
     * Constructs the default instance.
     * @since 2.9
     */
    public RuleParser() {
        this(true, RuleNodeSettings.RuleEngine);
    }

    /**
     * @param warnOnColRefsInStrings Warn on suspicious references in {@link String}s.
     * @param nodeType The {@link RuleNodeSettings}.
     */
    public RuleParser(final boolean warnOnColRefsInStrings, final RuleNodeSettings nodeType) {
        super(warnOnColRefsInStrings, nodeType);
        for (BooleanConstants constant : BooleanConstants.values()) {
            getOperators().add(constant.toString());
        }
    }
}
