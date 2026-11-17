package mod.a.example;

import com.povech.api.plugin.PluginContext;
import com.povech.api.service.llbot.LlbotService;

import java.util.Map;
import java.util.Objects;

/**
 * 通用 LLBOT action 入口。SDK 有专用服务时优先使用专用服务；
 * 尚未封装的 action 可通过这里传 action 名称和 params 调用。
 */
public class LlbotExample {
    private final PluginContext context;
    private final LlbotService llbotService;

    public LlbotExample(PluginContext context) {
        this.context = Objects.requireNonNull(context, "context");
        this.llbotService = context.llbot();
    }

    /** 获取 LLBOT 版本信息，适合作为通用调用的低风险示例。 */
    public void getVersion() {
        call("get_version", Map.of());
    }

    /** 查询群资料。action 与参数名按 LLBOT/OneBot 接口文档填写。 */
    public void getGroupInfo(long groupId) {
        call("get_group_info", Map.of("group_id", groupId));
    }

    /** 调用任意 action。写操作会真实作用于登录账号，调用前确认参数和权限。 */
    public void call(String action, Map<String, ?> params) {
        llbotService.call(action, params)
                .thenAccept(response -> context.logger().info(
                        "LLBOT action=" + action + " 响应字段=" + response.keySet()))
                .exceptionally(error -> {
                    context.logger().warn("LLBOT action 调用失败：" + error.getMessage());
                    return null;
                });
    }
}
