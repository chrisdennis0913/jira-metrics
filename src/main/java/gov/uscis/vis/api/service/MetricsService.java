package gov.uscis.vis.api.service;

import gov.uscis.vis.api.models.MetricsDto;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by cedennis on 3/10/17.
 */
public interface MetricsService {
    public Map<Integer, MetricsDto> analyzeBoard(ArrayList<Integer> boardList);
}
