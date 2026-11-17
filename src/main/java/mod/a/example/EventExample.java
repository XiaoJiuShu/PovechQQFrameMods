package mod.a.example;

import com.povech.api.event.EventTarget;
import com.povech.api.event.lifecycle.BotLifecycleEvent;
import com.povech.api.event.lifecycle.HeartbeatEvent;
import com.povech.api.event.lifecycle.LifecycleType;
import com.povech.api.event.message.GroupMessageEvent;
import com.povech.api.event.message.MessageEvent;
import com.povech.api.event.message.PrivateMessageEvent;
import com.povech.api.event.notice.*;
import com.povech.api.event.request.FriendRequestEvent;
import com.povech.api.event.request.GroupRequestEvent;
import com.povech.api.model.file.FlashFile;
import com.povech.api.model.notice.EmojiReaction;
import com.povech.api.model.user.Sender;
import com.povech.api.plugin.PluginContext;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * 事件示例：覆盖 SDK 全部 24 个事件类的字段。
 *
 * 分布：
 * event.message   3 个（MessageEvent 是基类，另两个是具体类型）
 * event.notice   17 个
 * event.request   2 个
 * event.lifecycle 2 个
 *
 * 用法要点：
 * 1. 监听方法加 @EventTarget，参数就是要监听的事件类型，方法必须是 public。
 * 2. 本类实例需要在 Initializer 里 eventBus.register(...)，卸载时 unregister(...)。
 * 3. 插件事件回调运行在本插件的单线程事件队列上。不要在里面阻塞等待
 *    CompletableFuture（get() / join()），否则会延迟本插件后续事件。
 * 4. 所有事件都有 timestamp() 与 botId()，下面不再重复注释。
 */
public class EventExample {
    private final PluginContext context;

    public EventExample(PluginContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    // ========================= 消息事件 3 个 =========================

    /**
     * MessageEvent 是群消息与私聊消息的共同基类，监听它可以同时收到两种消息。
     * 需要群号时请监听 GroupMessageEvent，基类上没有 groupId()。
     *
     * 字段共 6 个（GroupMessageEvent 与 PrivateMessageEvent 都继承这些）。
     */
    @EventTarget
    public void onMessage(MessageEvent event) {
        long timestamp = event.timestamp();  // 时间戳
        long botId = event.botId();          // 机器人QQ号
        long userId = event.userId();        // 发消息的人的QQ号
        long messageId = event.messageId();  // 消息序列号，撤回和引用回复要用
        Sender sender = event.sender();      // 发送者信息，字段见 printSender
        var message = event.message();       // 消息内容，遍历方式见 MessageExample

        context.logger().info(String.format("收到消息 来自=%d messageId=%d 段数=%d bot=%d 时间=%d",
                userId, messageId, message.segments().size(), botId, timestamp));
        printSender(sender);
    }

    /**
     * 群消息。相比基类多了 groupId()。
     */
    @EventTarget
    public void onGroupMessage(GroupMessageEvent event) {
        long groupId = event.groupId();      // 群号，这是 GroupMessageEvent 独有的字段
        context.logger().info("群 " + groupId + " 收到消息，发送者=" + event.userId());
    }

    /**
     * 私聊消息。字段与基类完全一致，没有新增字段。
     */
    @EventTarget
    public void onPrivateMessage(PrivateMessageEvent event) {
        context.logger().info("私聊消息，来自=" + event.userId());
    }

    /**
     * Sender 全部 9 个字段。私聊场景下 groupId 与群相关字段可能无意义。
     */
    private void printSender(Sender sender) {
        long userId = sender.userId();     // QQ号
        String nickname = sender.nickname(); // 昵称
        String card = sender.card();       // 群名片
        var sex = sender.sex();            // 性别
        int age = sender.age();            // 年龄
        String level = sender.level();     // 群等级，字符串
        var role = sender.role();          // 群身份，GroupRole
        String title = sender.title();     // 专属头衔
        long groupId = sender.groupId();   // 群号

        context.logger().info(String.format(
                "  发送者 %d 昵称=%s 名片=%s 性别=%s 年龄=%d 等级=%s 身份=%s 头衔=%s 群=%d",
                userId, nickname, card, sex, age, level, role, title, groupId));
    }

    // ========================= 通知事件 17 个 =========================

    /**
     * 1/17 群成员入群。
     */
    @EventTarget
    public void onGroupMemberJoined(GroupMemberJoinedEvent event) {
        long groupId = event.groupId();       // 群号
        long userId = event.userId();         // 入群成员QQ号
        long operatorId = event.operatorId(); // 操作者QQ号（审批人或邀请人）
        String joinType = event.joinType();   // 入群方式，如主动申请 / 被邀请

        context.logger().info(String.format("入群 群=%d 成员=%d 操作者=%d 方式=%s",
                groupId, userId, operatorId, joinType));
    }

    /**
     * 2/17 群成员退群或被踢。
     */
    @EventTarget
    public void onGroupMemberLeft(GroupMemberLeftEvent event) {
        long groupId = event.groupId();       // 群号
        long userId = event.userId();         // 离开成员QQ号
        long operatorId = event.operatorId(); // 操作者QQ号，主动退群时通常等于 userId
        String leaveType = event.leaveType(); // 离开方式，如主动退群 / 被踢

        context.logger().info(String.format("退群 群=%d 成员=%d 操作者=%d 方式=%s",
                groupId, userId, operatorId, leaveType));
    }

    /**
     * 3/17 管理员变动。
     */
    @EventTarget
    public void onGroupAdminChanged(GroupAdminChangedEvent event) {
        long groupId = event.groupId(); // 群号
        long userId = event.userId();   // 被操作成员QQ号
        boolean admin = event.admin();  // true 被设为管理员，false 被取消

        context.logger().info(String.format("管理员变动 群=%d 成员=%d 设为管理员=%s",
                groupId, userId, admin));
    }

    /**
     * 4/17 禁言状态变动。banned() 是便捷方法，等价于判断 duration 是否为正。
     */
    @EventTarget
    public void onGroupBanChanged(GroupBanChangedEvent event) {
        long groupId = event.groupId();       // 群号
        long userId = event.userId();         // 被禁言成员QQ号
        long operatorId = event.operatorId(); // 操作者QQ号
        Duration duration = event.duration(); // 禁言时长
        boolean banned = event.banned();      // true 是禁言，false 是解除禁言

        context.logger().info(String.format("禁言变动 群=%d 成员=%d 操作者=%d 时长=%s 禁言中=%s",
                groupId, userId, operatorId, duration, banned));
    }

    /**
     * 5/17 群名片被修改。
     */
    @EventTarget
    public void onGroupCardChanged(GroupCardChangedEvent event) {
        long groupId = event.groupId();   // 群号
        long userId = event.userId();     // 被改名片的成员QQ号
        String oldCard = event.oldCard(); // 修改前名片
        String newCard = event.newCard(); // 修改后名片

        context.logger().info(String.format("名片变动 群=%d 成员=%d %s -> %s",
                groupId, userId, oldCard, newCard));
    }

    /**
     * 6/17 群被解散。
     */
    @EventTarget
    public void onGroupDismissed(GroupDismissedEvent event) {
        long groupId = event.groupId(); // 被解散的群号
        long userId = event.userId();   // 操作者QQ号

        context.logger().warn("群 " + groupId + " 已被 " + userId + " 解散");
    }

    /**
     * 7/17 群消息被撤回。
     */
    @EventTarget
    public void onGroupMessageRecall(GroupMessageRecallEvent event) {
        long groupId = event.groupId();       // 群号
        long userId = event.userId();         // 原消息发送者QQ号
        long operatorId = event.operatorId(); // 撤回操作者QQ号
        long messageId = event.messageId();   // 被撤回的消息序列号

        context.logger().info(String.format("群消息撤回 群=%d 发送者=%d 操作者=%d messageId=%d",
                groupId, userId, operatorId, messageId));
    }

    /**
     * 8/17 私聊消息被撤回。
     */
    @EventTarget
    public void onFriendMessageRecall(FriendMessageRecallEvent event) {
        long userId = event.userId();       // 撤回消息的好友QQ号
        long messageId = event.messageId(); // 被撤回的消息序列号

        context.logger().info(String.format("私聊消息撤回 好友=%d messageId=%d", userId, messageId));
    }

    /**
     * 9/17 群精华消息变动。注意这里是 senderId 而不是 userId。
     */
    @EventTarget
    public void onGroupEssenceChanged(GroupEssenceChangedEvent event) {
        long groupId = event.groupId();       // 群号
        long senderId = event.senderId();     // 原消息发送者QQ号
        long operatorId = event.operatorId(); // 操作者QQ号
        long messageId = event.messageId();   // 相关消息序列号
        boolean added = event.added();        // true 设为精华，false 取消精华

        context.logger().info(String.format("精华变动 群=%d 发送者=%d 操作者=%d messageId=%d 设为精华=%s",
                groupId, senderId, operatorId, messageId, added));
    }

    /**
     * 10/17 群成员头衔变动。
     */
    @EventTarget
    public void onGroupTitleChanged(GroupTitleChangedEvent event) {
        long groupId = event.groupId(); // 群号
        long userId = event.userId();   // 成员QQ号
        String title = event.title();   // 新头衔

        context.logger().info(String.format("头衔变动 群=%d 成员=%d 头衔=%s", groupId, userId, title));
    }

    /**
     * 11/17 群消息表情回应（贴表情）。
     * reactions() 是 EmojiReaction 列表，每项含 emojiId 与 count。
     */
    @EventTarget
    public void onGroupEmojiReaction(GroupEmojiReactionEvent event) {
        long groupId = event.groupId();                  // 群号
        long userId = event.userId();                    // 操作者QQ号
        long messageId = event.messageId();              // 被回应的消息序列号
        List<EmojiReaction> reactions = event.reactions(); // 当前该消息上的全部表情统计
        boolean added = event.added();                   // true 新增回应，false 取消回应

        context.logger().info(String.format("表情回应 群=%d 操作者=%d messageId=%d 新增=%s",
                groupId, userId, messageId, added));
        for (EmojiReaction reaction : reactions) {
            // EmojiReaction 共 2 个字段：emojiId 表情ID，count 该表情的回应人数
            context.logger().info("  表情 " + reaction.emojiId() + " x" + reaction.count());
        }
    }

    /**
     * 12/17 群文件上传。file() 直接给出 GroupFile，字段见 FileExample。
     */
    @EventTarget
    public void onGroupFileUploaded(GroupFileUploadedEvent event) {
        long groupId = event.groupId(); // 群号
        long userId = event.userId();   // 上传者QQ号
        var file = event.file();        // 上传的文件，GroupFile

        context.logger().info(String.format("群文件上传 群=%d 上传者=%d 文件=%s 大小=%d",
                groupId, userId, file.name(), file.size()));
    }

    /**
     * 13/17 戳一戳。私聊场景下 groupId 通常为 0。
     */
    @EventTarget
    public void onPoke(PokeEvent event) {
        long userId = event.userId();     // 发起戳的人QQ号
        long targetId = event.targetId(); // 被戳的人QQ号
        long groupId = event.groupId();   // 群号，私聊为 0

        context.logger().info(String.format("戳一戳 发起=%d 目标=%d 群=%d", userId, targetId, groupId));

        // 典型用法：被戳的是机器人自己时回应
        if (context.bot() != null && targetId == context.bot().userId()) {
            context.logger().info("机器人被戳了");
        }
    }

    /**
     * 14/17 戳一戳撤回。字段与 PokeEvent 完全一致。
     */
    @EventTarget
    public void onPokeRecall(PokeRecallEvent event) {
        context.logger().info(String.format("戳一戳撤回 发起=%d 目标=%d 群=%d",
                event.userId(), event.targetId(), event.groupId()));
    }

    /**
     * 15/17 好友已添加（对方通过了机器人的请求，或请求被同意后建立好友关系）。
     */
    @EventTarget
    public void onFriendAdded(FriendAddedEvent event) {
        long userId = event.userId(); // 新好友QQ号
        context.logger().info("已添加好友：" + userId);
    }

    /**
     * 16/17 资料卡被点赞。
     */
    @EventTarget
    public void onProfileLiked(ProfileLikedEvent event) {
        long operatorId = event.operatorId();             // 点赞者QQ号
        String operatorNickname = event.operatorNickname(); // 点赞者昵称
        int times = event.times();                        // 点赞次数

        context.logger().info(String.format("收到点赞 来自=%s(%d) 次数=%d",
                operatorNickname, operatorId, times));
    }

    /**
     * 17/17 闪传文件事件，字段最多，带传输进度。
     */
    @EventTarget
    public void onFlashFile(FlashFileEvent event) {
        String title = event.title();               // 标题
        String shareLink = event.shareLink();       // 分享链接
        String fileSetId = event.fileSetId();       // 闪传集合ID
        List<FlashFile> files = event.files();      // 文件列表
        long downloadedSize = event.downloadedSize(); // 已下载字节数
        long uploadedSize = event.uploadedSize();   // 已上传字节数
        long totalSize = event.totalSize();         // 总字节数
        long speed = event.speed();                 // 速度，字节每秒
        long remainingSeconds = event.remainingSeconds(); // 预计剩余秒数

        context.logger().info(String.format(
                "闪传 标题=%s 集合=%s 链接=%s 进度 下载=%d 上传=%d 总计=%d 速度=%d/s 剩余=%ds",
                title, fileSetId, shareLink,
                downloadedSize, uploadedSize, totalSize, speed, remainingSeconds));

        for (FlashFile file : files) {
            // FlashFile 共 3 个字段：name 文件名，size 大小，path 本地路径
            context.logger().info(String.format("  %s 大小=%d 路径=%s",
                    file.name(), file.size(), file.path()));
        }
    }

    // ========================= 请求事件 2 个 =========================

    /**
     * 好友请求。处理方式见 MiscExample 中的 RequestService 用法，
     * 这里只列字段，避免和 MiscExample 重复处理同一条请求。
     */
    @EventTarget
    public void onFriendRequest(FriendRequestEvent event) {
        context.logger().info(String.format("好友请求 申请人=%d 验证=%s 来源=%s flag=%s",
                event.userId(), event.comment(), event.source(), event.flag()));
    }

    /**
     * 加群请求。
     */
    @EventTarget
    public void onGroupRequest(GroupRequestEvent event) {
        context.logger().info(String.format(
                "加群请求 群=%d 申请人=%d 邀请人=%d 来源群=%d 子类型=%s 验证=%s flag=%s",
                event.groupId(), event.userId(), event.inviterId(), event.sourceGroupId(),
                event.subType(), event.comment(), event.flag()));
    }

    // ========================= 生命周期事件 2 个 =========================

    /**
     * Bot 生命周期变化。type() 是 LifecycleType 枚举，共 4 项。
     */
    @EventTarget
    public void onBotLifecycle(BotLifecycleEvent event) {
        long botId = event.botId();        // 机器人QQ号
        LifecycleType type = event.type(); // 生命周期类型

        String description = switch (type) {
            case CONNECT -> "已连接";
            case ENABLE -> "已启用";
            case DISCONNECT -> "已断开";
            case UNKNOWN -> "未知";
        };
        context.logger().info("Bot " + botId + " " + description);
    }

    /**
     * 心跳。可用来做掉线检测，详见 SystemExample.onHeartbeat。
     */
    @EventTarget
    public void onHeartbeat(HeartbeatEvent event) {
        context.logger().debug(String.format("心跳 bot=%d 间隔=%dms 在线=%s",
                event.botId(), event.interval(), event.status().online()));
    }
}
