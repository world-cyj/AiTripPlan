package managerAgent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.DashScopeChatModel;
import managerAgent.agents.ManagerAgent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import utils.AgentUtils;

import java.util.List;

/**
 * author: Imooc
 * description: 启动类
 * date: 2026
 */

//@SpringBootApplication
public class ManagerAgentApplication {
    public static void main(String[] args) {

        ManagerAgent manager =  new ManagerAgent();
        manager.run();
//        SpringApplication.run(ManagerAgentApplication.class, args);
    }
}
