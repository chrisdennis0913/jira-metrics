package gov.uscis.vis.api.models;

import java.util.List;

/**
 * Created by cedennis on 1/30/17.
 */
public class SprintList {
    private int maxResults;
    private int startAt;
    private int total;
    private boolean isLast;
    private List<Sprint> values;

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public int getStartAt() {
        return startAt;
    }

    public void setStartAt(int startAt) {
        this.startAt = startAt;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public boolean isLast() {
        return isLast;
    }

    public void setLast(boolean last) {
        isLast = last;
    }

    public List<Sprint> getValues() {
        return values;
    }

    public void setValues(List<Sprint> values) {
        this.values = values;
    }
}
