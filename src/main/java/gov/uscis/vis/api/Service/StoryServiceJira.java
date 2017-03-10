package gov.uscis.vis.api.service;

import gov.uscis.vis.api.models.IssueList;
import gov.uscis.vis.api.models.SprintList;
import gov.uscis.vis.api.models.StateEnum;
import org.springframework.web.client.RestTemplate;

/**
 * Created by cedennis on 2/9/17.
 */
public class StoryServiceJira implements StoryService{
    private String adminKey;

    private RestTemplate restTemplate;

    public StoryServiceJira() {
        restTemplate = new RestTemplate();
        adminKey = "&key=4321";
    }
    public SprintList getSprintList(Integer boardId){
        String jiraRequestSprintUrl = "https://sharedservices.dhs.gov/jira/rest/agile/1.0/board/" + boardId;

        return restTemplate.getForObject(jiraRequestSprintUrl + adminKey, SprintList.class);
    }

    public SprintList getSprintListWithState(Integer boardId, StateEnum state){
        String jiraRequestSprintUrl = "https://sharedservices.dhs.gov/jira/rest/agile/1.0/board/"
                + boardId
                + "/sprint?state=" + state.getLabel() + "&order+by+endDate";

        return restTemplate.getForObject(jiraRequestSprintUrl + adminKey, SprintList.class);

    }

    public IssueList getIssueListForSprint(Long latestCompletedSprintId) {
        String jiraRequestIssuesUrl = "https://sharedservices.dhs.gov/jira/rest/agile/1.0/sprint/" + latestCompletedSprintId + "/issue";
        String restrictIssueFields = "&fields=id,key,status,resolution";
        return restTemplate.getForObject(jiraRequestIssuesUrl + adminKey + restrictIssueFields, IssueList.class);
    }
}
