package gov.uscis.vis.api;

import gov.uscis.vis.api.Service.StoryService;
import gov.uscis.vis.api.Service.StoryServiceSample;
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
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by cedennis on 1/30/17.
 */
@SpringBootApplication
public class JiraMetricsMain implements CommandLineRunner
{
    private static final Logger log = LoggerFactory.getLogger(JiraMetricsMain.class);

    private static Integer[] boardList = new Integer[]{722, 1332}; //everify board = 722, save mod =1332, vdm board = 853

    private static final int TOTAL_SPRINTS_TO_PROCESS = 5;

    private Map<Long, Map<PointTypeEnum, Double>> issueTypesMap; // issue type
    private Map<PointTypeEnum, Double> pointsMap; //completed vs total

    private StoryService storyService;

    public static void main(String[] args) {
        SpringApplication.run(JiraMetricsMain.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        storyService = new StoryServiceSample();
//        storyService = new StoryServiceJira();
        analyzeBoardList();
    }

    private void analyzeBoardList() {
        Map<Integer, MetricsDto> metricsMap = new HashMap<>();

        for (int boardId: boardList){
            initiateIssueTypesMap();
            MetricsDto boardMetrics = new MetricsDto(boardId);

            // 1) Story Point Forecast Accuracy
            // 6) Defect Fix Rate: Defects fixed / Defects in backlog
            SprintList closedSprintList = storyService.getSprintListWithState(boardId, StateEnum.CLOSED);
            Long latestCompletedSprintId = closedSprintList.getValues().stream()
                    .max((s1, s2) -> s1.getEndDate().compareTo(s2.getEndDate()))
                    .get().getId();
            IssueList issueList = storyService.getIssueListForSprint(latestCompletedSprintId);

//            List<Issue> storyList = new ArrayList<>(issueList.getIssues().size());
//            Collections.copy(issueList.getIssues(), storyList);

            Map<Long, Integer> completedStoriesFromClosedSprints = new HashMap<>();
            Map<Long, Double> completedStoryPointsFromClosedSprints = new HashMap<>();

            //issuetype: 1=Bug, 3=Task, 7=Story, 11200=Spike, 12102=Preview Defect, 12103=Production Defect
            extractMetricsFromIssueList(issueList, completedStoriesFromClosedSprints, completedStoryPointsFromClosedSprints);

            boardMetrics.setIssueForecastAccuracy(issueTypesMap.get(IssueTypeEnum.STORY.getId()).get(PointTypeEnum.COMPLETED_STORIES) * 100
                    /issueTypesMap.get(IssueTypeEnum.STORY.getId()).get(PointTypeEnum.TOTAL_STORIES));
            boardMetrics.setStoryPointForecastAccuracy(issueTypesMap.get(IssueTypeEnum.STORY.getId()).get(PointTypeEnum.COMPLETED_POINTS) * 100
                    /issueTypesMap.get(IssueTypeEnum.STORY.getId()).get(PointTypeEnum.TOTAL_POINTS));
            boardMetrics.setBugIssueForecastAccuracy((issueTypesMap.get(IssueTypeEnum.BUG.getId()).get(PointTypeEnum.COMPLETED_STORIES) + issueTypesMap.get(IssueTypeEnum.PREVIEW_DEFECT.getId()).get(PointTypeEnum.COMPLETED_STORIES) + issueTypesMap.get(IssueTypeEnum.PRODUCTION_DEFECT.getId()).get(PointTypeEnum.COMPLETED_STORIES)) * 100
                    /(issueTypesMap.get(IssueTypeEnum.BUG.getId()).get(PointTypeEnum.TOTAL_STORIES) + issueTypesMap.get(IssueTypeEnum.PREVIEW_DEFECT.getId()).get(PointTypeEnum.TOTAL_STORIES) + issueTypesMap.get(IssueTypeEnum.PRODUCTION_DEFECT.getId()).get(PointTypeEnum.TOTAL_STORIES)));
            boardMetrics.setBugStoryPointForecastAccuracy((issueTypesMap.get(IssueTypeEnum.BUG.getId()).get(PointTypeEnum.COMPLETED_POINTS) + issueTypesMap.get(IssueTypeEnum.PREVIEW_DEFECT.getId()).get(PointTypeEnum.COMPLETED_POINTS) + issueTypesMap.get(IssueTypeEnum.PRODUCTION_DEFECT.getId()).get(PointTypeEnum.COMPLETED_POINTS)) * 100
                    /(issueTypesMap.get(IssueTypeEnum.BUG.getId()).get(PointTypeEnum.TOTAL_POINTS) + issueTypesMap.get(IssueTypeEnum.PREVIEW_DEFECT.getId()).get(PointTypeEnum.TOTAL_POINTS) + issueTypesMap.get(IssueTypeEnum.PRODUCTION_DEFECT.getId()).get(PointTypeEnum.TOTAL_POINTS)));

            // 2) Story Point Completion Rate

            // More difficult than expected to calculate the story points completed for multiple previous sprints
            // Have to assume that an issue is updated if accessed from a previous sprint, even if completed in a later sprint
            // Need to recursively keep track of "subtract X amount of completed issues from Y past sprint"
            int sprintsToProcess = Math.min(closedSprintList.getTotal(), TOTAL_SPRINTS_TO_PROCESS);

            int[] storiesCompletedPerSprint     = new int[sprintsToProcess];
            double[] storyPointsCompletedPerSprint = new double[sprintsToProcess];
            storiesCompletedPerSprint[0] = getPointTypeTotal(issueTypesMap, PointTypeEnum.COMPLETED_STORIES);
            storyPointsCompletedPerSprint[0] = getPointTypeTotal(issueTypesMap, PointTypeEnum.COMPLETED_POINTS);

            Collections.sort(closedSprintList.getValues());
            for(int sprintIter = 1; sprintIter < sprintsToProcess; sprintIter ++){ //sprint 0 already processed
                int currentCompletedIssuesForSprint = 0;
                double currentCompletedPointsForSprint = 0;

                Sprint currentSprint = closedSprintList.getValues().get(sprintIter);
                IssueList currentIssueList = storyService.getIssueListForSprint(currentSprint.getId());
                List<Issue> listOfIssues = currentIssueList.getIssues().stream().filter(issue -> issue.getFields().getResolution() != null).collect(Collectors.toList());
                for(Issue currentIssue: listOfIssues){
                    currentCompletedIssuesForSprint++;
                    currentCompletedPointsForSprint += currentIssue.getFields().getCustomfield_10002() != null ? Double.valueOf(currentIssue.getFields().getCustomfield_10002()) : 1.0;
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

        System.out.println(metricsMap);
        log.error(metricsMap.toString());
    }

    public void extractMetricsFromIssueList(IssueList issueList, Map<Long, Integer> completedStoriesFromClosedSprints, Map<Long, Double> completedStoryPointsFromClosedSprints) {
        for (Issue issue : issueList.getIssues()){
            Field currentField = issue.getFields();
            IssueType currentIssueType = currentField.getIssuetype();
            incrementPointValue(currentIssueType, PointTypeEnum.TOTAL_STORIES, "1");
            incrementPointValue(currentIssueType, PointTypeEnum.TOTAL_POINTS, currentField.getCustomfield_10002());
            if (currentField.getResolution() != null && currentField.getResolution().getId().equals(7L)) {
                incrementPointValue(currentIssueType, PointTypeEnum.COMPLETED_STORIES, "1");
                incrementPointValue(currentIssueType, PointTypeEnum.COMPLETED_POINTS, currentField.getCustomfield_10002());

                if (currentIssueType != null && currentIssueType.getId().equals(IssueTypeEnum.STORY.getId())) {
                    if (currentField.getClosedSprints() != null && !currentField.getClosedSprints().isEmpty()) {
                        for (Sprint closedSprint : currentField.getClosedSprints()) {
                            int storyCount = 1;
                            double storyPointCount = currentField.getCustomfield_10002()!= null ? Double.valueOf(currentField.getCustomfield_10002()) : 0;
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

    private void incrementPointValue(IssueType issueType, PointTypeEnum pointTypeEnum, String stringValue){
        System.out.println(issueType.getName() + " pte: " + pointTypeEnum + " sv " + stringValue);
        double pointValue = 1;
        try{
            pointValue = Double.valueOf(stringValue);
        } catch (NullPointerException npe) {
            log.debug("issueType= " + issueType + ", pointTypeEnum= " + pointTypeEnum + " was null");
        }

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
