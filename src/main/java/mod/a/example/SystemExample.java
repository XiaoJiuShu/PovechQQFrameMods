package mod.a.example;

import com.povech.api.event.EventTarget;
import com.povech.api.event.lifecycle.HeartbeatEvent;
import com.povech.api.model.system.BotInfo;
import com.povech.api.model.system.BotStatus;
import com.povech.api.plugin.PluginContext;
import com.povech.api.plugin.PluginDescriptor;
import com.povech.api.plugin.PluginLogger;
import com.povech.api.result.system.VersionInfo;
import com.povech.api.service.system.SystemService;

import java.util.Objects;

/**
 * 官方分类：系统
 *
 * 覆盖 SystemService 的全部 3 个方法，外加 PluginContext 自身提供的
 * bot()、descriptor()、logger() 三个非服务入口。
 *
 * 未提供专用封装的 LLBOT action 可通过 LlbotExample 调用；Cookie、重启等
 * 高风险 action 不要在初始化或事件监听中自动调用。
 */
public class SystemExample {
    private final PluginContext context;
    private final SystemService systemService;

    public SystemExample(PluginContext context) {
        this.context = Objects.requireNonNull(context, "context");
        this.systemService = context.system();
    }

    // ================= SystemService 全部方法 =================

    /**
     * 获取机器人账号信息。异步版本，会向协议端实际请求。
     * 若只是想拿当前 QQ 号，用 context.bot() 更省一次请求。
     */
    public void getBotInfo() {
        systemService.getBotInfo()
                .thenAccept(bot -> context.logger().info(
                        "机器人 " + bot.userId() + " 昵称=" + bot.nickname()))
                .exceptionally(this::warn);
    }

    /**
     * 获取机器人运行状态。
     */
    public void getStatus() {
        systemService.getStatus()
                .thenAccept(this::printStatus)
                .exceptionally(this::warn);
    }

    /**
     * 获取协议端版本信息，可用来判断当前对接的实现是不是 LLOneBot 及其版本。
     */
    public void getVersionInfo() {
        systemService.getVersionInfo()
                .thenAccept(this::printVersion)
                .exceptionally(this::warn);
    }

    // ================= PluginContext 非服务入口 =================

    /**
     * context.bot() 同步返回框架缓存的机器人信息，不发请求。
     * 适合在消息处理里判断"这条消息是不是我自己发的"。
     */
    public boolean isSelf(long userId) {
        BotInfo bot = context.bot();
        return bot != null && bot.userId() == userId;
    }

    /**
     * context.descriptor() 返回当前插件自身的描述信息，
     * 其 4 个字段来自框架对插件的注册结果。
     */
    public void printDescriptor() {
        PluginDescriptor descriptor = context.descriptor();
        String id = descriptor.id();           // 插件ID
        String name = descriptor.name();       // 插件名称
        String version = descriptor.version(); // 插件版本
        String author = descriptor.author();   // 插件作者

        context.logger().info(String.format("插件 id=%s 名称=%s 版本=%s 作者=%s",
                id, name, version, author));
    }

    /**
     * PluginLogger 全部 5 个方法。
     *
     * 框架会输出插件的所有 API 相关操作，但会过滤以下四项，它们不会出现在框架日志里：
     * PluginContext.logger
     * PluginLogger.info
     * EventBus.register
     * EventBus.unregister
     */
    public void loggerLevels() {
        PluginLogger logger = context.logger();
        logger.debug("调试信息，排查问题时用");
        logger.info("常规信息");
        logger.warn("警告，不影响运行");
        logger.error("错误");
        logger.error("带异常堆栈的错误", new IllegalStateException("示例异常"));
    }

    // ================= 心跳事件 =================

    /**
     * HeartbeatEvent 会周期性触发，可用来做掉线检测。
     * interval() 是心跳间隔毫秒数，status() 复用 BotStatus 模型。
     */
    @EventTarget
    public void onHeartbeat(HeartbeatEvent event) {
        long timestamp = event.timestamp(); // 事件时间戳
        long botId = event.botId();         // 机器人QQ号
        long interval = event.interval();   // 心跳间隔，毫秒
        BotStatus status = event.status();  // 当前状态

        if (!status.online() || !status.healthy()) {
            context.logger().warn(String.format(
                    "心跳异常 bot=%d 在线=%s 健康=%s 间隔=%dms 时间=%d",
                    botId, status.online(), status.healthy(), interval, timestamp));
        }
    }

    // ================= 返回模型字段速查 =================

    /**
     * BotStatus 全部 2 个字段。
     */
    private void printStatus(BotStatus status) {
        boolean online = status.online();   // 是否在线
        boolean healthy = status.healthy(); // 协议端是否健康

        context.logger().info("在线=" + online + " 健康=" + healthy);
    }

    /**
     * VersionInfo 全部 3 个字段。
     */
    private void printVersion(VersionInfo version) {
        String applicationName = version.applicationName(); // 协议端名称，如 LLOneBot
        String appVersion = version.version();              // 协议端版本
        String protocolVersion = version.protocolVersion(); // 协议版本，如 v11

        context.logger().info(String.format("协议端=%s 版本=%s 协议=%s",
                applicationName, appVersion, protocolVersion));
    }

    private <T> T warn(Throwable error) {
        context.logger().warn("系统接口调用失败：" + error.getMessage());
        return null;
    }
}
