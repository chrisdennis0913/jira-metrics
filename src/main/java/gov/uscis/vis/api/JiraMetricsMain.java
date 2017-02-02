package gov.uscis.vis.api;

import gov.uscis.vis.api.models.Field;
import gov.uscis.vis.api.models.Issue;
import gov.uscis.vis.api.models.IssueList;
import gov.uscis.vis.api.models.Sprint;
import gov.uscis.vis.api.models.SprintList;
import gov.uscis.vis.api.models.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by cedennis on 1/30/17.
 */
public class JiraMetricsMain implements CommandLineRunner{
    private static final Logger log = LoggerFactory.getLogger(JiraMetricsMain.class);

    private static Integer[] boardList = new Integer[]{722, 1332}; //everify board = 722, save mod =1332

    private String adminKey;

    private RestTemplate restTemplate;

    private static final int TOTAL_SPRINTS_TO_PROCESS = 5;

    Map<Integer, Map<Long, Map<PointTypeEnum, Integer>>> boardsMap;

    @Override
    public void run(String... args) throws Exception {
        restTemplate = new RestTemplate();
        adminKey = "&key=4321";

        boardsMap = new HashMap<>(); //board

        Map<Long, Map<PointTypeEnum, Integer>> issueTypesMap; // issue type
        Map<PointTypeEnum, Integer> pointsMap; //completed vs total

        for (int boardId: boardList){//vdm board = 853
            issueTypesMap = new HashMap<>();
            pointsMap = new HashMap<>();

            boardsMap.get(722).get(IssueTypeEnum.BUG.getId()).get(PointTypeEnum.COMPLETED);

            // 1) Story Point Forecast Accuracy
            // 6) Defect Fix Rate: Defects fixed / Defects in backlog
            SprintList closedSprintList = getClosedSprintList(boardId);
            Long latestCompletedSprintId = closedSprintList.getValues().stream()
                    .max((s1, s2) -> s1.getEndDate().compareTo(s2.getEndDate()))
                    .get().getId();
            IssueList issueList = getIssueListForSprint(latestCompletedSprintId);

            List<Issue> storyList = new ArrayList<>(issueList.getIssues().size());
            Collections.copy(issueList.getIssues(), storyList);

            //

            Map<Long, Integer> completedStoriesFromClosedSprints = new HashMap<>();
            Map<Long, Integer> completedStoryPointsFromClosedSprints = new HashMap<>();
            int completedIssuesForStoriesInLatestSprint = 0;
            int completedPointsForStoriesInLatestSprint = 0;
            int totalIssuesPlannedForSprint = 0;
            int totalPointsPlannedForSprint = 0;
            int totalIssuesPlannedForStoriesInSprint = 0;
            int totalPointsPlannedForStoriesInSprint = 0;
            int completedIssuesForBugsInSprint = 0;
            int completedPointsForBugsInSprint = 0;
            int totalIssuesPlannedForBugsInSprint = 0;
            int totalPointsPlannedForBugsInSprint = 0;
            int totalIssuesPlannedForTasksInSprint = 0;
            int totalPointsPlannedForTasksInSprint = 0;
            int totalIssuesPlannedForPreviewDefectsInSprint = 0;
            int totalPointsPlannedForPreviewDefectsInSprint = 0;
            int completedIssuesForPreviewDefectsInSprint = 0;
            int completedPointsForPreviewDefectsInSprint = 0;
            int totalIssuesPlannedForProductionDefectsInSprint = 0;
            int totalPointsPlannedForProductionDefectsInSprint = 0;
            int completedIssuesForProductionDefectsInSprint = 0;
            int completedPointsForProductionDefectsInSprint = 0;
            // TODO: nullPointer check for customField_10002
            //issuetype: 1=Bug, 3=Task, 7=Story, 11200=Spike, 12102=Preview Defect, 12103=Production Defect
            for (Issue issue : storyList){
                Field currentField = issue.getFields();
//                incrementPointValue(currentField.getIssueType(), PointTypeEnum.TOTAL_STORIES, 1);
//                incrementPointValue(currentField.getIssueType(), PointTypeEnum.TOTAL_POINTS, currentField.getCustomField_10002());

                if (currentField.getIssueType() != null && currentField.getIssueType().getId().equals(IssueTypeEnum.BUG.getId())) {



                    totalIssuesPlannedForBugsInSprint ++;
                    totalPointsPlannedForBugsInSprint += Integer.valueOf(currentField.getCustomField_10002());
                    if (currentField.getResolution() != null) {
                        completedIssuesForBugsInSprint ++;
                        completedPointsForBugsInSprint += Integer.valueOf(currentField.getCustomField_10002());
                    }
                }
                else if (currentField.getIssueType() != null && currentField.getIssueType().getId().equals(IssueTypeEnum.TASK.getId())) {
                    totalIssuesPlannedForTasksInSprint ++;
                    totalPointsPlannedForTasksInSprint += Integer.valueOf(currentField.getCustomField_10002());
                } else if (currentField.getIssueType() != null && currentField.getIssueType().getId().equals(IssueTypeEnum.PREVIEW_DEFECT.getId())) {
                    totalIssuesPlannedForPreviewDefectsInSprint ++;
                    totalPointsPlannedForPreviewDefectsInSprint += Integer.valueOf(currentField.getCustomField_10002());
                    if (currentField.getResolution() != null) {
                        completedIssuesForPreviewDefectsInSprint ++;
                        completedPointsForPreviewDefectsInSprint += Integer.valueOf(currentField.getCustomField_10002());
                    }
                } else if (currentField.getIssueType() != null && currentField.getIssueType().getId().equals(IssueTypeEnum.PRODUCTION_DEFECT.getId())) {
                    totalIssuesPlannedForProductionDefectsInSprint ++;
                    totalPointsPlannedForProductionDefectsInSprint += Integer.valueOf(currentField.getCustomField_10002());
                    if (currentField.getResolution() != null) {
                        completedIssuesForProductionDefectsInSprint ++;
                        completedPointsForProductionDefectsInSprint += Integer.valueOf(currentField.getCustomField_10002());
                    }
                } else if (currentField.getIssueType() != null && currentField.getIssueType().getId().equals(IssueTypeEnum.SPIKE.getId())) {
                    continue;
                } else if (currentField.getIssueType() != null && currentField.getIssueType().getId().equals(IssueTypeEnum.STORY.getId())) {
                    totalIssuesPlannedForStoriesInSprint ++;
                    totalPointsPlannedForStoriesInSprint += Integer.valueOf(currentField.getCustomField_10002());
                    if (currentField.getResolution() != null) {
                        completedIssuesForStoriesInLatestSprint ++;
                        completedPointsForStoriesInLatestSprint += Integer.valueOf(currentField.getCustomField_10002());

                        if (currentField.getClosedSprints() != null && !currentField.getClosedSprints().isEmpty()){
                            for (Sprint closedSprint :currentField.getClosedSprints()){
                                int storyCount      = 1;
                                int storyPointCount = Integer.valueOf(currentField.getCustomField_10002());
                                if (completedStoriesFromClosedSprints.containsKey(closedSprint.getId())){
                                    storyCount      += completedStoriesFromClosedSprints.get(closedSprint.getId());
                                    storyPointCount += completedStoryPointsFromClosedSprints.get(closedSprint.getId());
                                }
                                completedStoriesFromClosedSprints.put(closedSprint.getId(), storyCount);
                                completedStoryPointsFromClosedSprints.put(closedSprint.getId(), storyPointCount);
                            }
                        }
                    }
                }



                totalPointsPlannedForSprint += Integer.valueOf(currentField.getCustomField_10002());
                totalIssuesPlannedForSprint++;

            }

            double issueForecastAccuracy = completedIssuesForStoriesInLatestSprint/totalIssuesPlannedForStoriesInSprint;
            double storyPointForecastAccuracy = completedPointsForStoriesInLatestSprint/totalPointsPlannedForStoriesInSprint;
            double bugIssueForecastAccuracy = (completedIssuesForBugsInSprint + completedIssuesForPreviewDefectsInSprint + completedIssuesForProductionDefectsInSprint)
                    /(totalIssuesPlannedForBugsInSprint + totalIssuesPlannedForPreviewDefectsInSprint + totalIssuesPlannedForProductionDefectsInSprint);
            double bugStoryPointForecastAccuracy = (completedPointsForBugsInSprint + completedPointsForPreviewDefectsInSprint + completedPointsForProductionDefectsInSprint)
                    /(totalPointsPlannedForBugsInSprint + totalPointsPlannedForPreviewDefectsInSprint +totalPointsPlannedForProductionDefectsInSprint);

            // 2) Story Point Completion Rate
            Integer storyPointsCompletedForLatestSprint = completedPointsForStoriesInLatestSprint;
            Integer issuesCompletedForLatestSprint = completedIssuesForStoriesInLatestSprint;
            // More difficult than expected to calculate the story points completed for multiple previous sprints
            // Have to assume that an issue is updated if accessed from a previous sprint, even if completed in a later sprint
            // Need to recursively keep track of "subtract X amount of completed issues from Y past sprint"

            int sprintsToProcess = Math.min(closedSprintList.getTotal(), TOTAL_SPRINTS_TO_PROCESS);

            int[] storiesCompletedPerSprint     = new int[sprintsToProcess];
            int[] storyPointsCompletedPerSprint = new int[sprintsToProcess];
            storiesCompletedPerSprint[0] = completedIssuesForStoriesInLatestSprint;
            storyPointsCompletedPerSprint[0] = completedPointsForStoriesInLatestSprint;

            Collections.sort(closedSprintList.getValues());
            for(int sprintIter = 1; sprintIter < sprintsToProcess; sprintIter ++){ //sprint 0 already processed
                int currentCompletedIssuesForSprint = 0;
                int currentCompletedPointsForSprint = 0;

                Sprint currentSprint = closedSprintList.getValues().get(sprintIter);
                IssueList currentIssueList = getIssueListForSprint(currentSprint.getId());
                List<Issue> listOfIssues = currentIssueList.getIssues().stream().filter(issue -> issue.getFields().getResolution() != null).collect(Collectors.toList());
                for(Issue currentIssue: listOfIssues){
                    currentCompletedIssuesForSprint++;
                    currentCompletedPointsForSprint += Integer.valueOf(currentIssue.getFields().getCustomField_10002());
                }

                currentCompletedIssuesForSprint -= completedStoriesFromClosedSprints.get(currentSprint.getId());
                currentCompletedPointsForSprint -= completedStoryPointsFromClosedSprints.get(currentSprint.getId());

                storiesCompletedPerSprint[sprintIter] = currentCompletedIssuesForSprint;
                storyPointsCompletedPerSprint[sprintIter] = currentCompletedPointsForSprint;
            }



            //// 3) Unit Test Coverage- have to be done in code
            //// 4) % of automated acceptance Test cases- have to be done in code

            // 5) Sprint work breakdown: (New User Stories + product enhancement user stories) / (technical debt user stories + production incidents + other user stories)
            // story totals / tasks + bugs + spikes + preview defects + production defects

            double sprintWorkBreakdownIssues =  completedIssuesForStoriesInLatestSprint /
            double sprintWorkBreakdownPoints = completedPointsForStoriesInLatestSprint /

            //// 7) % of top Business Value features completed- forecast accuracy
            //// 8) Cost per release / planned cost per release must be done outside of Jira
        }

    }

    private SprintList getSprintList(Integer boardId){
        String jiraRequestSprintUrl = "https://sharedservices.dhs.gov/jira/rest/agile/1.0/board/"
                + boardId
                + "/sprint?state=closed";

        return restTemplate.getForObject(jiraRequestSprintUrl + adminKey, SprintList.class);
    }

    private SprintList getClosedSprintList(Integer boardId){
        String jiraRequestSprintUrl = "https://sharedservices.dhs.gov/jira/rest/agile/1.0/board/" + boardId;

        return restTemplate.getForObject(jiraRequestSprintUrl + adminKey, SprintList.class);
    }

    private IssueList getIssueListForSprint(Long latestCompletedSprintId) {
        String jiraRequestIssuesUrl = "https://sharedservices.dhs.gov/jira/rest/agile/1.0/sprint/" + latestCompletedSprintId + "/issue";
        String restrictIssueFields = "&fields=id,key,status,resolution";
        return restTemplate.getForObject(jiraRequestIssuesUrl + adminKey + restrictIssueFields, IssueList.class);
    }
}
