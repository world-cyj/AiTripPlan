package utils;

import io.agentscope.core.tool.Toolkit;

/**
 * author: Imooc
 * description: Agent Tool 工具类
 * date: 2026
 */

public class ToolUtils {

    private final Toolkit toolkit ;

    public ToolUtils() {
        //创建工具包
        toolkit = new Toolkit();
    }

    /**
     * author: Imooc
     * description: 获取工具包
     * @param Tool:
     * @return io.agentscope.core.tool.Toolkit
     */
    public Toolkit getToolkit(Object Tool) {

        //把工具添加到工具包，能自动扫描@Tool所注释的方法，作为Agent的工具
        toolkit.registerTool(Tool);

        return toolkit;
    }
}
