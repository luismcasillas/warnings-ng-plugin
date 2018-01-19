package io.jenkins.plugins.analysis.warnings;

import java.util.Collection;

import edu.hm.hafner.analysis.AbstractParser;
import edu.hm.hafner.analysis.parser.ResharperInspectCodeParser;
import io.jenkins.plugins.analysis.core.model.AbstractParserTool;
import io.jenkins.plugins.analysis.core.model.DefaultLabelProvider;
import io.jenkins.plugins.analysis.core.model.StaticAnalysisLabelProvider;

import hudson.Extension;

/**
 * Provides a parser and customized messages for Resharper Inspections.
 *
 * @author Ullrich Hafner
 */
@Extension
public class ResharperInspectCode extends AbstractParserTool {
    private static final String ID = "resharper";
    private static final String PARSER_NAME = Messages.Warnings_ReshaperInspectCode_ParserName();

    @Override
    public Collection<? extends AbstractParser> getParsers() {
        return only(new ResharperInspectCodeParser());
    }

    @Override
    public StaticAnalysisLabelProvider getLabelProvider() {
        return new LabelProvider();
    }

    /** Provides the labels for the static analysis tool. */
    private static class LabelProvider extends DefaultLabelProvider {
        private LabelProvider() {
            super(ID, PARSER_NAME);
        }
    }
}