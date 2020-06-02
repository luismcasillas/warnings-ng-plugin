package io.jenkins.plugins.analysis.warnings;

import java.util.Collection;
import java.util.List;

import org.junit.Test;

import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;
import org.jenkinsci.test.acceptance.junit.WithPlugins;
import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.FreeStyleJob;

import io.jenkins.plugins.analysis.warnings.AnalysisResult.Tab;
import io.jenkins.plugins.analysis.warnings.IssuesDetailsTable.Header;

import static io.jenkins.plugins.analysis.warnings.Assertions.*;

/**
 * Integration tests for the details tab part of issue overview page.
 *
 * @author Nils Engelbrecht
 * @author Kevin Richter
 * @author Simon Schönwiese
 */
@WithPlugins("warnings-ng")
public class DetailsTabUiTest extends AbstractJUnitTest {
    private static final String WARNINGS_PLUGIN_PREFIX = "/details_tab_test/";

    /**
     * When a single warning is being recognized only the issues-tab should be shown.
     */
    @Test
    public void shouldPopulateDetailsTabSingleWarning() {
        FreeStyleJob job = createFreeStyleJob("java1Warning.txt");
        job.addPublisher(IssuesRecorder.class, recorder -> recorder.setToolWithPattern("Java", "**/*.txt"));
        job.save();

        Build build = job.startBuild().waitUntilFinished();
        assertThat(build.isSuccess()).isTrue();

        AnalysisResult resultPage = new AnalysisResult(build, "java");
        resultPage.open();

        Collection<Tab> tabs = resultPage.getAvailableTabs();
        assertThat(tabs).containsOnlyOnce(Tab.ISSUES);
        assertThat(resultPage.getActiveTab()).isEqualTo(Tab.ISSUES);

        IssuesDetailsTable issuesDetailsTable = resultPage.openIssuesTable();
        assertThat(issuesDetailsTable.getTableRows()).hasSize(1);
    }

    /**
     * When two warnings are being recognized in one file the tabs issues, files and folders should be shown.
     */
    @Test
    public void shouldPopulateDetailsTabMultipleWarnings() {
        FreeStyleJob job = createFreeStyleJob("java2Warnings.txt");
        job.addPublisher(IssuesRecorder.class, recorder -> recorder.setToolWithPattern("Java", "**/*.txt"));
        job.save();

        Build build = job.startBuild().waitUntilFinished();
        assertThat(build.isSuccess()).isTrue();

        AnalysisResult resultPage = new AnalysisResult(build, "java");
        resultPage.open();

        assertThat(resultPage).hasOnlyAvailableTabs(Tab.FOLDERS, Tab.FILES, Tab.ISSUES);

        PropertyDetailsTable foldersDetailsTable = resultPage.openPropertiesTable(Tab.FOLDERS);
        assertThat(foldersDetailsTable).hasTotal(2);

        PropertyDetailsTable filesDetailsTable = resultPage.openPropertiesTable(Tab.FILES);
        assertThat(filesDetailsTable).hasTotal(2);

        IssuesDetailsTable issuesDetailsTable = resultPage.openIssuesTable();
        assertThat(issuesDetailsTable).hasTotal(2);
    }

    /**
     * When switching details-tab and the page is being reloaded, the previously selected tab should be memorized and
     * still be active.
     */
    @Test
    public void shouldMemorizeSelectedTabAsActiveOnPageReload() {
        FreeStyleJob job = createFreeStyleJob("../checkstyle-result.xml");
        job.addPublisher(IssuesRecorder.class, recorder -> recorder.setTool("CheckStyle"));
        job.save();

        Build build = job.startBuild().waitUntilFinished();
        assertThat(build.isSuccess()).isTrue();

        AnalysisResult resultPage = new AnalysisResult(build, "checkstyle");
        resultPage.open();

        assertThat(resultPage).hasOnlyAvailableTabs(Tab.ISSUES, Tab.TYPES, Tab.CATEGORIES);

        assertThat(resultPage.getActiveTab()).isNotEqualTo(Tab.TYPES);
        resultPage.openTab(Tab.TYPES);
        assertThat(resultPage.getActiveTab()).isEqualTo(Tab.TYPES);

        resultPage.reload();
        assertThat(resultPage.getActiveTab()).isEqualTo(Tab.TYPES);
    }

    /**
     * When having a larger checkstyle result, the table should display all Tabs, tables and pages correctly and should
     * be able to change the page.
     */
    @Test
    public void shouldWorkWithMultipleTabsAndPages() {
        FreeStyleJob job = createFreeStyleJob("../checkstyle-result.xml");
        job.addPublisher(IssuesRecorder.class, recorder -> recorder.setTool("CheckStyle"));
        job.save();

        Build build = job.startBuild().waitUntilFinished();
        assertThat(build.isSuccess()).isTrue();

        AnalysisResult resultPage = new AnalysisResult(build, "checkstyle");
        resultPage.open();

        assertThat(resultPage).hasOnlyAvailableTabs(Tab.ISSUES, Tab.TYPES, Tab.CATEGORIES);

        PropertyDetailsTable categoriesDetailsTable = resultPage.openPropertiesTable(Tab.CATEGORIES);
        assertThat(categoriesDetailsTable).hasHeaders("Category", "Total", "Distribution");
        assertThat(categoriesDetailsTable).hasSize(5).hasTotal(5);

        PropertyDetailsTable typesDetailsTable = resultPage.openPropertiesTable(Tab.TYPES);
        assertThat(typesDetailsTable).hasHeaders("Type", "Total", "Distribution");
        assertThat(typesDetailsTable).hasSize(7).hasTotal(7);

        IssuesDetailsTable issuesDetailsTable = resultPage.openIssuesTable();
        assertThat(issuesDetailsTable).hasColumnHeaders(Header.DETAILS, Header.FILE, Header.CATEGORY,
                Header.TYPE, Header.SEVERITY, Header.AGE);
        assertThat(issuesDetailsTable).hasSize(10).hasTotal(11);

        List<GenericTableRow> tableRowListIssues = issuesDetailsTable.getTableRows();
        IssuesTableRow firstRow = (IssuesTableRow) tableRowListIssues.get(0);
        firstRow.toggleDetailsRow();

        issuesDetailsTable.openTablePage(2);
        assertThat(issuesDetailsTable.getSize()).isEqualTo(1);

        tableRowListIssues = issuesDetailsTable.getTableRows();
        IssuesTableRow lastIssueTableRow = (IssuesTableRow) tableRowListIssues.get(0);
        assertThat(lastIssueTableRow.getSeverity()).isEqualTo("Error");
        AnalysisResult analysisResult = lastIssueTableRow.clickOnSeverityLink();
        IssuesDetailsTable errorIssuesDetailsTable = analysisResult.openIssuesTable();
        assertThat(errorIssuesDetailsTable.getSize()).isEqualTo(6);
        for (int i = 0; i < errorIssuesDetailsTable.getSize(); i++) {
            IssuesTableRow row = (IssuesTableRow) errorIssuesDetailsTable.getTableRows().get(i);
            assertThat(row.getSeverity()).isEqualTo("Error");
        }
    }

    private FreeStyleJob createFreeStyleJob(final String... resourcesToCopy) {
        FreeStyleJob job = jenkins.getJobs().create(FreeStyleJob.class);
        ScrollerUtil.hideScrollerTabBar(driver);
        for (String resource : resourcesToCopy) {
            job.copyResource(WARNINGS_PLUGIN_PREFIX + resource);
        }
        return job;
    }
}