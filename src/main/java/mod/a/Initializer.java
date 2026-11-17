package mod.a;

import com.povech.api.event.EventBus;
import com.povech.api.plugin.PluginContext;
import com.povech.api.plugin.PluginInitializer;
import com.povech.api.utils.WebUtils;
import mod.a.example.EventExample;
import mod.a.example.FileExample;
import mod.a.example.GroupExample;
import mod.a.example.LlbotExample;
import mod.a.example.MessageExample;
import mod.a.example.MiscExample;
import mod.a.example.SettingsExample;
import mod.a.example.SystemExample;
import mod.a.example.UserExample;

import javax.swing.JFrame;
import java.util.ArrayList;
import java.util.List;

public class Initializer implements PluginInitializer {
    private EventBus eventBus;
    private SettingsExample settingsExample;
    private UserExample userExample;
    private GroupExample groupExample;
    private FileExample fileExample;
    private LlbotExample llbotExample;
    private final List<Object> listeners = new ArrayList<>();

    @Override
    public String getPluginName() {
        return "示例";
    }

    @Override
    public String getVersion() {
        return "0";
    }

    @Override
    public String getDevName() {
        return "Xiao_Jiu_Shu, ImZhizi";
    }

    @Override
    public String getInformation() {
        return "这是Xiao_Jiu_Shu和ImZhizi的示例插件";
    }

    @Override
    public String getLastestVersion() {
        return WebUtils.get("https://raw.giteeusercontent.com/little-redemption/povech-qqframe/raw/master/LastestVersionID");
    }

    @Override
    public void onInitialization(PluginContext context) {
        eventBus = context.events();
        settingsExample = new SettingsExample(context);

        // 带 @EventTarget 的示例必须注册到事件总线才会收到事件。
        // 这四个示例与 EventExample 有部分事件重叠（群消息、心跳、好友与加群请求），
        // 注册在一起会看到重复日志，按需删掉不用的那个。
        listeners.add(new MessageExample(context));
        listeners.add(new MiscExample(context, settingsExample));
        listeners.add(new SystemExample(context));
        listeners.add(new EventExample(context));
        listeners.forEach(eventBus::register);

        userExample = new UserExample(context);
        groupExample = new GroupExample(context);
        fileExample = new FileExample(context);
        llbotExample = new LlbotExample(context);
        context.logger().info("示例插件已加载，事件监听器 " + listeners.size() + " 个");
    }

    /**
     * 设置界面入口。这是 PluginInitializer 上的 default 方法，默认返回 null，
     * 表示插件没有设置界面。返回 null 是合法的，不覆盖此方法即可。
     */
    @Override
    public JFrame createSettingsFrame() {
        // 理论上框架应在 onInitialization 之后才让用户打开设置，
        // 但这里不假设调用顺序，未初始化时返回 null 走"无设置界面"的默认约定。
        return settingsExample == null ? null : settingsExample.getOrCreateFrame();
    }

    @Override
    public void onDisable() {
        if (eventBus != null) {
            listeners.forEach(eventBus::unregister);
        }
        listeners.clear();
        if (settingsExample != null) {
            settingsExample.dispose();
            settingsExample = null;
        }
        userExample = null;
        groupExample = null;
        fileExample = null;
        llbotExample = null;
        eventBus = null;
    }
}
