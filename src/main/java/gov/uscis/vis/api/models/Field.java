package gov.uscis.vis.api.models;

import java.util.List;

/**
 * Created by cedennis on 1/30/17.
 */
public class Field {
    private Resolution resolution;
    private IssueType issueType;
    private Status status;
    private String customField_10002;
    private List<Sprint> closedSprints;

    public Resolution getResolution() {
        return resolution;
    }

    public void setResolution(Resolution resolution) {
        this.resolution = resolution;
    }

    public IssueType getIssueType() {
        return issueType;
    }

    public void setIssueType(IssueType issueType) {
        this.issueType = issueType;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getCustomField_10002() {
        return customField_10002;
    }

    public void setCustomField_10002(String customField_10002) {
        this.customField_10002 = customField_10002;
    }

    public List<Sprint> getClosedSprints() {
        return closedSprints;
    }

    public void setClosedSprints(List<Sprint> closedSprints) {
        this.closedSprints = closedSprints;
    }
}
