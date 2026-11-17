package mod.a.example;

import com.povech.api.model.message.Message;
import com.povech.api.plugin.PluginContext;
import com.povech.api.service.message.MessageService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Objects;

/**
 * 插件设置界面示例，对应 PluginInitializer.createSettingsFrame()。
 *
 * 这个方法是 PluginInitializer 上的 default 方法，默认实现直接返回 null，
 * 也就是"本插件没有设置界面"。想要设置界面就在 Initializer 里覆盖它，返回一个 JFrame。
 *
 * 桌面端在 Swing EDT 上调用此方法，随后负责显示窗口，并将关闭操作设为
 * DISPOSE_ON_CLOSE。因此窗口关闭后必须重新创建，不能复用已 dispose 的 JFrame。
 *
 * 设置项这里只存在内存里。SDK 没有暴露插件配置目录，需要持久化得自己选路径写文件。
 */
public class SettingsExample {
    private final PluginContext context;
    private final MessageService messageService;

    /** 缓存窗口实例，避免每次打开设置都新建一个 */
    private JFrame frame;

    // ---- 设置项，仅存在内存中 ----
    private volatile long targetGroupId;
    private volatile boolean autoApproveFriendRequest;

    public SettingsExample(PluginContext context) {
        this.context = Objects.requireNonNull(context, "context");
        this.messageService = context.messages();
    }

    /**
     * 供 Initializer.createSettingsFrame() 调用。
     */
    public JFrame getOrCreateFrame() {
        if (frame == null || !frame.isDisplayable()) {
            frame = buildFrame();
        }
        return frame;
    }

    private JFrame buildFrame() {
        JFrame settings = new JFrame("示例插件设置");
        settings.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JTextField groupIdField = new JTextField(targetGroupId == 0 ? "" : String.valueOf(targetGroupId));
        JCheckBox autoApproveBox = new JCheckBox("自动同意好友请求", autoApproveFriendRequest);
        JLabel status = new JLabel("就绪");

        JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        form.add(new JLabel("目标群号："));
        form.add(groupIdField);
        form.add(new JLabel("好友请求："));
        form.add(autoApproveBox);

        JButton save = new JButton("保存");
        JButton sendTest = new JButton("发送测试消息");

        // ActionListener 在 EDT 上执行，读写组件是安全的
        save.addActionListener(e -> {
            Long parsed = parseGroupId(groupIdField.getText());
            if (parsed == null) {
                status.setText("群号必须是数字");
                return;
            }
            targetGroupId = parsed;
            autoApproveFriendRequest = autoApproveBox.isSelected();
            context.logger().info("设置已保存：群号=" + targetGroupId
                    + " 自动同意好友请求=" + autoApproveFriendRequest);
            status.setText("已保存");
        });

        sendTest.addActionListener(e -> {
            Long parsed = parseGroupId(groupIdField.getText());
            if (parsed == null || parsed == 0) {
                status.setText("请先填写有效群号");
                return;
            }
            status.setText("发送中");
            messageService.sendGroupMessage(parsed, Message.text("来自设置界面的测试消息"))
                    .thenAccept(result ->
                            // 关键：thenAccept 的回调不在 EDT 上，改动 Swing 组件
                            // 必须切回 EDT，否则会出现界面不刷新或错乱
                            SwingUtilities.invokeLater(() ->
                                    status.setText("已发送 messageId=" + result.messageId())))
                    .exceptionally(error -> {
                        SwingUtilities.invokeLater(() -> status.setText("发送失败：" + error.getMessage()));
                        return null;
                    });
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(save);
        buttons.add(sendTest);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        content.add(form);
        content.add(Box.createVerticalStrut(8));
        content.add(buttons);

        settings.setLayout(new BorderLayout());
        settings.add(content, BorderLayout.CENTER);
        settings.add(status, BorderLayout.SOUTH);
        settings.setMinimumSize(new Dimension(360, 200));
        settings.pack();
        settings.setLocationRelativeTo(null);
        return settings;
    }

    /**
     * 群号来自用户手输，必须校验。返回 null 表示输入非法。
     */
    private Long parseGroupId(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty()) {
            return 0L;
        }
        try {
            long value = Long.parseLong(trimmed);
            return value < 0 ? null : value;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    // ---- 供其他示例读取设置值 ----

    public long targetGroupId() {
        return targetGroupId;
    }

    /**
     * MiscExample 使用这个开关；默认关闭，打开后会自动同意好友请求。
     */
    public boolean autoApproveFriendRequest() {
        return autoApproveFriendRequest;
    }

    /**
     * 插件卸载时释放窗口资源。Initializer.onDisable() 里应当调用。
     */
    public void dispose() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }
}
