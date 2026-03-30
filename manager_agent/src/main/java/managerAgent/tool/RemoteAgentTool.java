package managerAgent.tool;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.ai.AiFactory;
import com.alibaba.nacos.api.ai.AiService;
import com.alibaba.nacos.api.exception.NacosException;
import io.agentscope.core.a2a.agent.A2aAgent;
import io.agentscope.core.nacos.a2a.discovery.NacosAgentCardResolver;
import io.agentscope.core.tool.Tool;
import utils.NacosUtil;

import java.util.Properties;

/**
 * author: Imooc
 * description: 将远程Agent封装为工具
 * date: 2026
 */

public class RemoteAgentTool {

    /**
     * author: Imooc
     * description: 基于A2A协议获取路线制定Agent
     * @param :
     * @return void
     */
    @Tool
    public void callRouteMakingAgent() throws NacosException {


        A2aAgent agent = A2aAgent.builder()
                .name("RouteMakingAgent")
                .agentCardResolver(
                        //创建 Nacos 的 AgentCardResolver
                        new NacosAgentCardResolver(NacosUtil.getNacosClient()))
                .build();


        //远程Agent运行
        agent.call().block();
    }

    /**
     * author: Imooc
     * description: 基于A2A协议获取行程规划Agent
     * @param :
     * @return void
     */
    @Tool
    public void callTripPlannerAgent() throws NacosException {

        A2aAgent agent = A2aAgent.builder()
                .name("TripPlannerAgent")
                .agentCardResolver(
                        //创建 Nacos 的 AgentCardResolver
                        new NacosAgentCardResolver(NacosUtil.getNacosClient()))
                .build();


        //远程Agent运行
        agent.call().block();

    }
}
