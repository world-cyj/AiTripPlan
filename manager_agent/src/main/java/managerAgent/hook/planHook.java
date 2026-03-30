package managerAgent.hook;

import io.agentscope.core.hook.*;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * author: Imooc
 * description:  计划拦截器
 * date: 2026
 */

@Slf4j
public class planHook implements Hook {
    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {

        /* **********************
         *
         * Hook 是对 HookEvent事件 的拦截
         * HookEvent事件：
         *
         * PreReasoningEvent：用户的输入事件
         * PostReasoningEvent: Agent推理思考过程事件
         * PreActingEvent： Agent执行过程准备调用工具的事件
         * PostActingEvent：Agent执行过程调用工具完成的事件
         *
         *
         * *********************/

        //匹配不同的事件
        switch (event) {

            //用户输入事件
            case PreReasoningEvent e -> {

                String reason = e.getInputMessages().get(0).getTextContent();
                log.info("#### 用户的Prompt：#######" );
                log.info(reason);

            }

            //推理思考事件
            case PostReasoningEvent e -> {

                String reason = e.getReasoningMessage().getTextContent();
                log.info("#### 思考过程：#######" );
                log.info(reason);

            }


            //调用工具事件
            case PostActingEvent e -> {

                String toolName = e.getToolUse().getName();
                log.info("##### 调用工具："+toolName);
            }

            default -> {
                // 其他事件忽略
            }
        }

        // 返回原事件
        return Mono.just(event);
    }
}
