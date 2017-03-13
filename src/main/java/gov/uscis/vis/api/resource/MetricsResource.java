package gov.uscis.vis.api.resource;

import gov.uscis.vis.api.models.MetricsDto;
import gov.uscis.vis.api.service.MetricsService;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by cedennis on 3/10/17.
 */
@Path("/board")
@Api("/board")
@Component
public class MetricsResource {

    private static final Logger log = LoggerFactory.getLogger(MetricsResource.class);

    @Autowired
    private MetricsService metricsService;

    @GET
    @Path("/analyze")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<Integer, MetricsDto> analyzeJiraBoard( @QueryParam("boardList") ArrayList<Integer> boardList) {
        return metricsService.analyzeBoard(boardList);
    }

}
