package gov.uscis.vis.api.models;

import java.util.Arrays;

/**
 * Created by cedennis on 2/3/17.
 */
public class MetricsDto {

    int boardId;

    // 1) Story Point Forecast Accuracy
    double issueForecastAccuracy;
    double storyPointForecastAccuracy;

    // 2) Story Point Completion Rate
    int[] storiesCompletedPerSprint;
    double[] storyPointsCompletedPerSprint;

    // 5) Sprint work breakdown: (New User Stories + product enhancement user stories) / (technical debt user stories + production incidents + other user stories)
    // story totals + tasks + spikes / bugs + preview defects + production defects
    double sprintWorkBreakdownIssues;
    double sprintWorkBreakdownPoints;

    // 6) Defect Fix Rate: Defects fixed / Defects in backlog
    double bugIssueForecastAccuracy;
    double bugStoryPointForecastAccuracy;

    public MetricsDto(int boardId) {
        this.boardId = boardId;
    }

    public int getBoardId() {
        return boardId;
    }

    public void setBoardId(int boardId) {
        this.boardId = boardId;
    }

    public double getIssueForecastAccuracy() {
        return issueForecastAccuracy;
    }

    public void setIssueForecastAccuracy(double issueForecastAccuracy) {
        this.issueForecastAccuracy = issueForecastAccuracy;
    }

    public double getStoryPointForecastAccuracy() {
        return storyPointForecastAccuracy;
    }

    public void setStoryPointForecastAccuracy(double storyPointForecastAccuracy) {
        this.storyPointForecastAccuracy = storyPointForecastAccuracy;
    }

    public int[] getStoriesCompletedPerSprint() {
        return storiesCompletedPerSprint;
    }

    public void setStoriesCompletedPerSprint(int[] storiesCompletedPerSprint) {
        this.storiesCompletedPerSprint = storiesCompletedPerSprint;
    }

    public double[] getStoryPointsCompletedPerSprint() {
        return storyPointsCompletedPerSprint;
    }

    public void setStoryPointsCompletedPerSprint(double[] storyPointsCompletedPerSprint) {
        this.storyPointsCompletedPerSprint = storyPointsCompletedPerSprint;
    }

    public double getSprintWorkBreakdownIssues() {
        return sprintWorkBreakdownIssues;
    }

    public void setSprintWorkBreakdownIssues(double sprintWorkBreakdownIssues) {
        this.sprintWorkBreakdownIssues = sprintWorkBreakdownIssues;
    }

    public double getSprintWorkBreakdownPoints() {
        return sprintWorkBreakdownPoints;
    }

    public void setSprintWorkBreakdownPoints(double sprintWorkBreakdownPoints) {
        this.sprintWorkBreakdownPoints = sprintWorkBreakdownPoints;
    }

    public double getBugIssueForecastAccuracy() {
        return bugIssueForecastAccuracy;
    }

    public void setBugIssueForecastAccuracy(double bugIssueForecastAccuracy) {
        this.bugIssueForecastAccuracy = bugIssueForecastAccuracy;
    }

    public double getBugStoryPointForecastAccuracy() {
        return bugStoryPointForecastAccuracy;
    }

    public void setBugStoryPointForecastAccuracy(double bugStoryPointForecastAccuracy) {
        this.bugStoryPointForecastAccuracy = bugStoryPointForecastAccuracy;
    }

    @Override
    public String toString() {
        return "MetricsDto{" +
                "boardId=" + boardId +
                ", issueForecastAccuracy=" + issueForecastAccuracy +
                ", storyPointForecastAccuracy=" + storyPointForecastAccuracy +
                ", storiesCompletedPerSprint=" + Arrays.toString(storiesCompletedPerSprint) +
                ", storyPointsCompletedPerSprint=" + Arrays.toString(storyPointsCompletedPerSprint) +
                ", sprintWorkBreakdownIssues=" + sprintWorkBreakdownIssues +
                ", sprintWorkBreakdownPoints=" + sprintWorkBreakdownPoints +
                ", bugIssueForecastAccuracy=" + bugIssueForecastAccuracy +
                ", bugStoryPointForecastAccuracy=" + bugStoryPointForecastAccuracy +
                '}';
    }
}