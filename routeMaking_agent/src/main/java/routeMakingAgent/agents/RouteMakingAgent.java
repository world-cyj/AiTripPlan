package routeMakingAgent.agents;

import io.agentscope.core.ReActAgent;
import utils.AgentUtils;

/**
 * author: Imooc
 * description: TODO
 * date: 2026
 */

public class RouteMakingAgent {

    public ReActAgent getRouteMakingAgent() {
        return AgentUtils.getReActAgentBuilder(
                "RouteMakingAgent",
                "路线制定Agent"
        ).build();
    }
}
