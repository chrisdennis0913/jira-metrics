package gov.uscis.vis.api.models;

import java.util.List;

/**
 * Created by cedennis on 1/30/17.
 */
public class Issue {
    private Long id;
    private Field fields;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Field getFields() {
        return fields;
    }

    public void setFields(Field fields) {
        this.fields = fields;
    }
}
