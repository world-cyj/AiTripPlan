package tripPlannerAgent.agents;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.a2a.server.AgentScopeA2aServer;
import io.agentscope.core.a2a.server.card.ConfigurableAgentCard;
import utils.AgentUtils;

/**
 * author: Imooc
 * description: 行程规划Agent
 * date: 2026
 */

public class TripPlannerAgent {

    public void getTripPlannerAgent() {

        //行程规划Agent Builder
        ReActAgent.Builder builder = AgentUtils.getReActAgentBuilder(
                "TripPlannerAgent",
                "行程规划Agent"
        );


        //行程规划Agent 智能体卡片
        ConfigurableAgentCard agentCard =  new ConfigurableAgentCard.Builder()
                .name("TripPlannerAgent")
                .description("行程规划Agent")
                .build();

        //将智能体卡片自动注册到Nacos
        AgentScopeA2aServer.builder(builder)
                .agentCard(agentCard)
                .build();

    }
}
