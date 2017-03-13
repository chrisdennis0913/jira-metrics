package gov.uscis.vis.api.resource;

import gov.uscis.vis.api.models.MetricsDto;
import gov.uscis.vis.api.service.MetricsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
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

    @POST
    @Path("/analyze")
    @ApiOperation(value = "Pulls metrics for a Jira board specified by ids. SAVE Mod is 1332, Everify is 722")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<Integer, MetricsDto> analyzeJiraBoard( @ApiParam(name = "boardList", value = "List of Board Ids") List<Integer> boardList) {
        if (boardList == null || boardList.isEmpty()){
            boardList = new ArrayList<>();
            boardList.add(722);
            boardList.add(1332);
        }
        return metricsService.analyzeBoard(boardList);
    }

}
