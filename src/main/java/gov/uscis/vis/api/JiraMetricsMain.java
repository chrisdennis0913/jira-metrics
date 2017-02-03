package gov.uscis.vis.api;

import gov.uscis.vis.api.models.Field;
import gov.uscis.vis.api.models.Issue;
import gov.uscis.vis.api.models.IssueList;
import gov.uscis.vis.api.models.IssueType;
import gov.uscis.vis.api.models.MetricsDto;
import gov.uscis.vis.api.models.Sprint;
import gov.uscis.vis.api.models.SprintList;
import gov.uscis.vis.api.models.StateEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.web.client.RestTemplate;

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

    private static Integer[] boardList = new Integer[]{722, 1332}; //everify board = 722, save mod =1332, vdm board = 853

    private String adminKey;

    private RestTemplate restTemplate;

    private static final int TOTAL_SPRINTS_TO_PROCESS = 5;

    private Map<Long, Map<PointTypeEnum, Double>> issueTypesMap; // issue type
    private Map<PointTypeEnum, Double> pointsMap; //completed vs total

    @Override
    public void run(String... args) throws Exception {
        restTemplate = new RestTemplate();
        adminKey = "&key=4321";

        Map<Integer, MetricsDto> metricsMap = new HashMap<>();

        for (int boardId: boardList){
            initiateIssueTypesMap();
            MetricsDto boardMetrics = new MetricsDto();

            // 1) Story Point Forecast Accuracy
            // 6) Defect Fix Rate: Defects fixed / Defects in backlog
            SprintList closedSprintList = getSprintListWithState(boardId, StateEnum.CLOSED);
            Long latestCompletedSprintId = closedSprintList.getValues().stream()
                    .max((s1, s2) -> s1.getEndDate().compareTo(s2.getEndDate()))
                    .get().getId();
            IssueList issueList = getIssueListForSprint(latestCompletedSprintId);

//            List<Issue> storyList = new ArrayList<>(issueList.getIssues().size());
//            Collections.copy(issueList.getIssues(), storyList);

            Map<Long, Integer> completedStoriesFromClosedSprints = new HashMap<>();
            Map<Long, Integer> completedStoryPointsFromClosedSprints = new HashMap<>();

            //issuetype: 1=Bug, 3=Task, 7=Story, 11200=Spike, 12102=Preview Defect, 12103=Production Defect
            for (Issue issue : issueList.getIssues()){
                Field currentField = issue.getFields();
                IssueType currentIssueType = currentField.getIssueType();
                incrementPointValue(currentIssueType, PointTypeEnum.TOTAL_STORIES, "1");
                incrementPointValue(currentIssueType, PointTypeEnum.TOTAL_POINTS, currentField.getCustomField_10002());
                if (currentField.getResolution() != null && currentField.getResolution().getId().equals(7L)) {
                    incrementPointValue(currentIssueType, PointTypeEnum.COMPLETED_STORIES, "1");
                    incrementPointValue(currentIssueType, PointTypeEnum.COMPLETED_POINTS, currentField.getCustomField_10002());

                    if (currentIssueType != null && currentIssueType.getId().equals(IssueTypeEnum.STORY.getId())) {
                        if (currentField.getClosedSprints() != null && !currentField.getClosedSprints().isEmpty()) {
                            for (Sprint closedSprint : currentField.getClosedSprints()) {
                                int storyCount = 1;
                                int storyPointCount = Integer.valueOf(currentField.getCustomField_10002());
                                if (completedStoriesFromClosedSprints.containsKey(closedSprint.getId())) {
                                    storyCount += completedStoriesFromClosedSprints.get(closedSprint.getId());
                                    storyPointCount += completedStoryPointsFromClosedSprints.get(closedSprint.getId());
                                }
                                completedStoriesFromClosedSprints.put(closedSprint.getId(), storyCount);
                                completedStoryPointsFromClosedSprints.put(closedSprint.getId(), storyPointCount);
                            }
                        }
                    }
                }
            }

            boardMetrics.setIssueForecastAccuracy(issueTypesMap.get(IssueTypeEnum.STORY.getId()).get(PointTypeEnum.COMPLETED_STORIES)
                    /issueTypesMap.get(IssueTypeEnum.STORY.getId()).get(PointTypeEnum.TOTAL_STORIES));
            boardMetrics.setStoryPointForecastAccuracy(issueTypesMap.get(IssueTypeEnum.STORY.getId()).get(PointTypeEnum.COMPLETED_POINTS)
                    /issueTypesMap.get(IssueTypeEnum.STORY.getId()).get(PointTypeEnum.TOTAL_POINTS));
            boardMetrics.setBugIssueForecastAccuracy((issueTypesMap.get(IssueTypeEnum.BUG.getId()).get(PointTypeEnum.COMPLETED_STORIES) + issueTypesMap.get(IssueTypeEnum.PREVIEW_DEFECT.getId()).get(PointTypeEnum.COMPLETED_STORIES) + issueTypesMap.get(IssueTypeEnum.PRODUCTION_DEFECT.getId()).get(PointTypeEnum.COMPLETED_STORIES))
                    /(issueTypesMap.get(IssueTypeEnum.BUG.getId()).get(PointTypeEnum.TOTAL_STORIES) + issueTypesMap.get(IssueTypeEnum.PREVIEW_DEFECT.getId()).get(PointTypeEnum.TOTAL_STORIES) + issueTypesMap.get(IssueTypeEnum.PRODUCTION_DEFECT.getId()).get(PointTypeEnum.TOTAL_STORIES)));
            boardMetrics.setBugStoryPointForecastAccuracy((issueTypesMap.get(IssueTypeEnum.BUG.getId()).get(PointTypeEnum.COMPLETED_POINTS) + issueTypesMap.get(IssueTypeEnum.PREVIEW_DEFECT.getId()).get(PointTypeEnum.COMPLETED_POINTS) + issueTypesMap.get(IssueTypeEnum.PRODUCTION_DEFECT.getId()).get(PointTypeEnum.COMPLETED_POINTS))
                    /(issueTypesMap.get(IssueTypeEnum.BUG.getId()).get(PointTypeEnum.TOTAL_POINTS) + issueTypesMap.get(IssueTypeEnum.PREVIEW_DEFECT.getId()).get(PointTypeEnum.TOTAL_POINTS) + issueTypesMap.get(IssueTypeEnum.PRODUCTION_DEFECT.getId()).get(PointTypeEnum.TOTAL_POINTS)));

            // 2) Story Point Completion Rate

            // More difficult than expected to calculate the story points completed for multiple previous sprints
            // Have to assume that an issue is updated if accessed from a previous sprint, even if completed in a later sprint
            // Need to recursively keep track of "subtract X amount of completed issues from Y past sprint"
            int sprintsToProcess = Math.min(closedSprintList.getTotal(), TOTAL_SPRINTS_TO_PROCESS);

            int[] storiesCompletedPerSprint     = new int[sprintsToProcess];
            int[] storyPointsCompletedPerSprint = new int[sprintsToProcess];
            storiesCompletedPerSprint[0] = getPointTypeTotal(issueTypesMap, PointTypeEnum.COMPLETED_STORIES);
            storyPointsCompletedPerSprint[0] = getPointTypeTotal(issueTypesMap, PointTypeEnum.COMPLETED_POINTS);

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

            boardMetrics.setStoriesCompletedPerSprint(storiesCompletedPerSprint);
            boardMetrics.setStoryPointsCompletedPerSprint(storyPointsCompletedPerSprint);

            //// 3) Unit Test Coverage- have to be done in application
            //// 4) % of automated acceptance Test cases- have to be done in application

            // 5) Sprint work breakdown: (New User Stories + product enhancement user stories) / (technical debt user stories + production incidents + other user stories)
            // story totals + tasks + spikes / bugs + preview defects + production defects
            boardMetrics.setSprintWorkBreakdownIssues((issueTypesMap.get(IssueTypeEnum.STORY.getId()).get(PointTypeEnum.TOTAL_STORIES) + issueTypesMap.get(IssueTypeEnum.TASK.getId()).get(PointTypeEnum.TOTAL_STORIES) + issueTypesMap.get(IssueTypeEnum.SPIKE.getId()).get(PointTypeEnum.TOTAL_STORIES))
                    / (issueTypesMap.get(IssueTypeEnum.BUG.getId()).get(PointTypeEnum.TOTAL_STORIES) + issueTypesMap.get(IssueTypeEnum.PREVIEW_DEFECT.getId()).get(PointTypeEnum.TOTAL_STORIES)+issueTypesMap.get(IssueTypeEnum.PRODUCTION_DEFECT.getId()).get(PointTypeEnum.TOTAL_STORIES)));
            boardMetrics.setSprintWorkBreakdownPoints((issueTypesMap.get(IssueTypeEnum.STORY.getId()).get(PointTypeEnum.TOTAL_POINTS) + issueTypesMap.get(IssueTypeEnum.TASK.getId()).get(PointTypeEnum.TOTAL_POINTS) + issueTypesMap.get(IssueTypeEnum.SPIKE.getId()).get(PointTypeEnum.TOTAL_POINTS))
                    / (issueTypesMap.get(IssueTypeEnum.BUG.getId()).get(PointTypeEnum.TOTAL_POINTS) + issueTypesMap.get(IssueTypeEnum.PREVIEW_DEFECT.getId()).get(PointTypeEnum.TOTAL_POINTS)+issueTypesMap.get(IssueTypeEnum.PRODUCTION_DEFECT.getId()).get(PointTypeEnum.TOTAL_POINTS)));

            //// 7) % of top Business Value features completed- forecast accuracy
            //// 8) Cost per release / planned cost per release must be done outside of Jira

            metricsMap.put(boardId, boardMetrics);
        }
    }

    private void initiateIssueTypesMap() {
        issueTypesMap = new HashMap<>();
        for (IssueTypeEnum issueTypeEnum : IssueTypeEnum.values()){
            pointsMap = new HashMap<>();
            for (PointTypeEnum pointTypeEnum : PointTypeEnum.values()){
                pointsMap.put(pointTypeEnum, 0.0);
            }
            issueTypesMap.put(issueTypeEnum.getId(), pointsMap);
        }
    }

    private SprintList getSprintList(Integer boardId){
        String jiraRequestSprintUrl = "https://sharedservices.dhs.gov/jira/rest/agile/1.0/board/" + boardId;

        return restTemplate.getForObject(jiraRequestSprintUrl + adminKey, SprintList.class);
    }

    private SprintList getSprintListWithState(Integer boardId, StateEnum state){
        String jiraRequestSprintUrl = "https://sharedservices.dhs.gov/jira/rest/agile/1.0/board/"
                + boardId
                + "/sprint?state=" + state.getLabel();

        return restTemplate.getForObject(jiraRequestSprintUrl + adminKey, SprintList.class);
    }

    private IssueList getIssueListForSprint(Long latestCompletedSprintId) {
        String jiraRequestIssuesUrl = "https://sharedservices.dhs.gov/jira/rest/agile/1.0/sprint/" + latestCompletedSprintId + "/issue";
        String restrictIssueFields = "&fields=id,key,status,resolution";
        return restTemplate.getForObject(jiraRequestIssuesUrl + adminKey + restrictIssueFields, IssueList.class);
    }

    private void incrementPointValue(IssueType issueType, PointTypeEnum pointTypeEnum, String stringValue){
        double pointValue = 1;
        try{
            pointValue = Double.valueOf(stringValue);
        } catch (NullPointerException npe) {log.debug("issueType= " + issueType + ", pointTypeEnum= " + pointTypeEnum + " was null");}

        pointsMap = issueTypesMap.get(issueType.getId());
        if (pointsMap == null) pointsMap = new HashMap<>();
        if (pointsMap.containsKey(pointTypeEnum)){
            pointValue += pointsMap.get(pointTypeEnum);
        }
        pointsMap.put(pointTypeEnum, pointValue);

        issueTypesMap.put(issueType.getId(), pointsMap);
    }

    private int getPointTypeTotal(Map<Long, Map<PointTypeEnum,Double>> issueTypeMap, PointTypeEnum pointTypeEnum) {
        int pointTypeTotal = 0;
        for (Map<PointTypeEnum,Double> pointTypeEnumMap : issueTypeMap.values()) {
            if (pointTypeEnumMap.containsKey(pointTypeEnum)) {
                pointTypeTotal += pointTypeEnumMap.get(pointTypeEnum);
            }
        }
        return pointTypeTotal;
    }
}
