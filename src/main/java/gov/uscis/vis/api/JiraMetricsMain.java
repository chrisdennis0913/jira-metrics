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

    @Override
    public void run(String... args) throws Exception {
        restTemplate = new RestTemplate();
        adminKey = "&key=4321";

        int boardId = boardList[0]; //vdm board = 853

        // 1) Story Point Forecast Accuracy
        // 6) Defect Fix Rate: Defects fixed / Defects in backlog
        SprintList sprintList = getSprintList(boardId);
        Long latestCompletedSprintId = sprintList.getValues().stream()
                .filter(sprint -> sprint.getState() != null && sprint.getState().compareTo(State.CLOSED) == 0)
                .max((s1, s2) -> s1.getEndDate().compareTo(s2.getEndDate())).get().getId();
        IssueList issueList = getIssueListForSprint(latestCompletedSprintId);

        List<Issue> storyList = new ArrayList<>(issueList.getIssues().size());
        Collections.copy(issueList.getIssues(), storyList);

        // 

        Map<Long, Integer> completedStoriesFromClosedSprints = new HashMap<>();
        Map<Long, Integer> completedStoryPointsFromClosedSprints = new HashMap<>();
        int completedIssuesForLatestSprint = 0;
        int completedPointsForLatestSprint = 0;
        int totalIssuesPlannedForSprint = 0;
        int totalPointsPlannedForSprint = 0;
        int completedIssuesForBugsInSprint = 0;
        int completedPointsForBugsInSprint = 0;
        int totalIssuesPlannedForBugsInSprint = 0;
        int totalPointsPlannedForBugsInSprint = 0;
        //nullPointer check for customField_10002
        for (Issue issue : storyList){
            Field currentField = issue.getFields();
            if (currentField.getResolution() != null) {
                completedIssuesForLatestSprint ++;
                completedPointsForLatestSprint += Integer.valueOf(currentField.getCustomField_10002());

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

                if (currentField.getIssueType() != null && currentField.getIssueType().getId().equals(1L)) {
                    completedPointsForBugsInSprint += Integer.valueOf(currentField.getCustomField_10002());
                    completedIssuesForBugsInSprint ++;
                }
            }

            totalPointsPlannedForSprint += Integer.valueOf(currentField.getCustomField_10002());
            totalIssuesPlannedForSprint++;
            if (currentField.getIssueType() != null && currentField.getIssueType().getId().equals(1L)) {
                totalPointsPlannedForBugsInSprint += Integer.valueOf(currentField.getCustomField_10002());
                totalIssuesPlannedForBugsInSprint ++;
            }
        }

        double storyPointForecastAccuracy = completedPointsForLatestSprint/totalPointsPlannedForSprint;
        double issueForecastAccuracy = completedIssuesForLatestSprint/totalIssuesPlannedForSprint;
        double bugStoryPointForecastAccuracy = completedPointsForBugsInSprint/totalPointsPlannedForBugsInSprint;
        double bugIssueForecastAccuracy = completedIssuesForBugsInSprint/totalIssuesPlannedForBugsInSprint;

        // 2) Story Point Completion Rate
        Integer storyPointsCompletedForLatestSprint = completedPointsForLatestSprint;
        Integer issuesCompletedForLatestSprint = completedIssuesForLatestSprint;
        // More difficult than expected to calculate the story points completed for multiple previous sprints
        // Have to assume that an issue is updated if accessed from a previous sprint, even if completed in a later sprint
        // Need to recursively keep track of "subtract X amount of completed issues from Y past sprint"

        int sprintsToProcess = Math.min(sprintList.getTotal(), TOTAL_SPRINTS_TO_PROCESS);

        int[] storiesCompletedPerSprint     = new int[sprintsToProcess];
        int[] storyPointsCompletedPerSprint = new int[sprintsToProcess];
        storiesCompletedPerSprint[0] = completedIssuesForLatestSprint;
        storyPointsCompletedPerSprint[0] = completedPointsForLatestSprint;

        Collections.sort(sprintList.getValues());
        for(int sprintIter = 1; sprintIter < sprintsToProcess; sprintIter ++){ //sprint 0 already processed
            int currentCompletedIssuesForSprint = 0;
            int currentCompletedPointsForSprint = 0;

            Sprint currentSprint = sprintList.getValues().get(sprintIter);
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


        double sprintWorkBreakdownIssues =  /
        double sprintWorkBreakdownPoints = completedPointsForLatestSprint /

        //// 7) % of top Business Value features completed- forecast accuracy
        //// 8) Cost per release / planned cost per release must be done outside of Jira
    }

    private SprintList getSprintList(Integer boardId){
        String jiraRequestSprintUrl = "https://sharedservices.dhs.gov/jira/rest/agile/1.0/board/" + boardId;

        return restTemplate.getForObject(jiraRequestSprintUrl + adminKey, SprintList.class);
    }

    private IssueList getIssueListForSprint(Long latestCompletedSprintId) {
        String jiraRequestIssuesUrl = "https://sharedservices.dhs.gov/jira/rest/agile/1.0/sprint/" + latestCompletedSprintId + "/issue";
        String restrictIssueFields = "&fields=id,key,status,resolution";
        return restTemplate.getForObject(jiraRequestIssuesUrl + adminKey + restrictIssueFields, IssueList.class);
    }
}
