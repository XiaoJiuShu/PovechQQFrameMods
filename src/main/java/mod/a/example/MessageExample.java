package mod.a.example;

import com.povech.api.event.EventTarget;
import com.povech.api.event.message.GroupMessageEvent;
import com.povech.api.exception.ApiException;
import com.povech.api.model.message.*;
import com.povech.api.plugin.PluginContext;
import com.povech.api.service.message.MessageService;

import java.util.List;
import java.util.Objects;

/**
 * 官方分类：消息
 *
 * 覆盖 MessageService 的全部 3 个方法，以及 MessageSegmentType 中全部 23 种消息段的构造参数。
 *
 * 说明：
 * 1. Service 的所有方法都返回 CompletableFuture。事件监听器异步运行，但在监听器中
 *    get()/join() 仍会阻塞该插件的事件队列；用 thenAccept / exceptionally 处理。
 * 2. 下列消息段的参数名取自 record 的访问器，与实际构造参数顺序严格一致。
 */
public class MessageExample {
    private final PluginContext context;
    private final MessageService messageService;

    public MessageExample(PluginContext context) {
        this.context = Objects.requireNonNull(context, "context");
        this.messageService = context.messages();
    }

    // ================= MessageService 全部方法 =================

    /**
     * 发送群消息。返回 SendMessageResult，其 messageId() 是发出去的消息序列号，
     * 后续撤回或被引用回复都要用它。
     */
    public void sendGroupMessage(long groupId) {
        messageService.sendGroupMessage(groupId, Message.text("发到群里的一句话"))
                .thenAccept(result -> context.logger().info("群消息已发送，messageId=" + result.messageId()))
                .exceptionally(this::logFailure);
    }

    /**
     * 发送私聊消息。userId 是对方 QQ 号。
     */
    public void sendPrivateMessage(long userId) {
        messageService.sendPrivateMessage(userId, Message.text("发给个人的一句话"))
                .thenAccept(result -> context.logger().info("私聊消息已发送，messageId=" + result.messageId()))
                .exceptionally(this::logFailure);
    }

    /**
     * 撤回消息。只能撤回机器人有权限撤回的消息（自己发的，或自己是管理员时的群成员消息）。
     * 无权限时 future 会以 ApiPermissionDeniedException 失败。
     */
    public void deleteMessage(long messageId) {
        messageService.deleteMessage(messageId)
                .thenRun(() -> context.logger().info("已撤回 messageId=" + messageId))
                .exceptionally(this::logFailure);
    }

    // ================= 构造 Message 的三种方式 =================

    public void buildMessage(long groupId) {
        // 方式一：纯文本，最常用
        Message a = Message.text("只有文本");

        // 方式二：可变参数拼多个消息段
        Message b = Message.of(
                new AtSegment("10001", null),
                new TextSegment(" 你被提到了")
        );

        // 方式三：传 List，适合消息段是动态拼出来的
        Message c = new Message(List.of(
                new TextSegment("第一段"),
                new TextSegment("第二段")
        ));

        // 空消息，isEmpty() 为 true。发送空消息没有意义，一般用来做占位或判空
        Message empty = Message.empty();
        context.logger().info("empty.isEmpty()=" + empty.isEmpty()
                + " a=" + a.segments().size()
                + " b=" + b.segments().size()
                + " c=" + c.segments().size());

        messageService.sendGroupMessage(groupId, b).exceptionally(this::logFailure);
    }

    /**
     * 读取收到的消息内容：遍历消息段，按 type() 分支处理。
     * type() 返回 MessageSegmentType 枚举，比 instanceof 更适合做 switch。
     */
    @EventTarget
    public void onGroupMessage(GroupMessageEvent event) {
        for (MessageSegment segment : event.message().segments()) {
            switch (segment.type()) {
                case TEXT -> context.logger().info("文本：" + ((TextSegment) segment).text());
                case IMAGE -> context.logger().info("图片：" + ((ImageSegment) segment).url());
                case AT -> context.logger().info("At：" + ((AtSegment) segment).target());
                case REPLY -> context.logger().info("回复了：" + ((ReplySegment) segment).messageId());
                case FACE -> context.logger().info("表情 id：" + ((FaceSegment) segment).id());
                default -> context.logger().info("其他消息段：" + segment.type());
            }
        }
    }

    // ================= 全部 23 种消息段速查 =================

    /**
     * MessageSegmentType 共 23 项，此方法把每一种的构造参数列全。
     * 参数顺序与 record 组件顺序一致，可直接照抄。
     */
    public void allSegments() {
        // TEXT  文本。text-文本内容
        MessageSegment text = new TextSegment("文本内容");

        // IMAGE 图片。file-文件名，url-图片URL，fileSize-文件大小，summary-外显文字（如"[动画表情]"），
        //       subType-子类型，imageType-图片类型，thumbnail-缩略图URL，name-图片名称
        MessageSegment image = new ImageSegment(
                "a.png", "https://example.com/a.png", "10240",
                "[图片]", 0, "png", null, "a.png");

        // MUSIC 音乐分享。musicType-平台(163/qq等)，id-歌曲ID，url-跳转链接，audio-音频直链，
        //       title-标题，content-描述，image-封面图
        MessageSegment music = new MusicSegment(
                "163", "123456", "https://example.com/song",
                "https://example.com/song.mp3", "歌名", "歌手", "https://example.com/cover.jpg");

        // VIDEO 视频。file-文件名，url-视频URL，path-本地路径，fileSize-文件大小，
        //       thumbnail-缩略图URL，name-视频名称
        // 注意：共 6 个参数，第 4 个是 fileSize
        MessageSegment video = new VideoSegment(
                "v.mp4", "https://example.com/v.mp4", "D:/v.mp4", "1048576", null, "v.mp4");

        // RECORD 语音。file-文件名，url-音频URL，path-本地路径，fileSize-文件大小，
        //        thumbnail-缩略图URL，name-音频名称
        // 注意：共 6 个参数，第 4 个是 fileSize
        MessageSegment record = new RecordSegment(
                "r.amr", "https://example.com/r.amr", "D:/r.amr", "4096", null, "r.amr");

        // FILE 文件。file-文件名，url-文件URL，path-本地路径，fileSize-文件大小，
        //      fileId-文件UUID，thumbnail-缩略图URL，name-文件名称
        MessageSegment file = new FileSegment(
                "doc.zip", "https://example.com/doc.zip", "D:/doc.zip",
                "20480", "uuid-0001", null, "doc.zip");

        // FLASH_FILE 闪传文件。title-标题，fileSetId-闪传集合ID，sceneType-场景类型
        MessageSegment flashFile = new FlashFileSegment("闪传标题", "set-0001", 1);

        // AT 提到某人。target-被At的QQ号（字符串），name-发出后显示的名字
        MessageSegment at = new AtSegment("10001", "某人");
        // At 全体成员，等价于 target 与 name 都填 all
        MessageSegment atAll = AtSegment.all();

        // REPLY 回复。messageId-被回复消息的序列号（long）
        MessageSegment reply = new ReplySegment(123456789L);

        // JSON 卡片。data-JSON 字符串
        MessageSegment json = new JsonSegment("{\"app\":\"com.tencent.miniapp\"}");

        // XML 卡片。data-XML 字符串
        MessageSegment xml = new XmlSegment("<msg><item/></msg>");

        // FACE QQ 原生表情。id-表情ID
        MessageSegment face = new FaceSegment("178");

        // MFACE 商城表情包。packageId-表情包ID，emojiId-表情ID，key-表情密钥，
        //       summary-外显名称，url-表情URL
        MessageSegment mface = new MFaceSegment(233, "emoji-1", "key-1", "[贴纸]", null);

        // MARKDOWN Markdown 内容。content-Markdown 正文
        MessageSegment markdown = new MarkdownSegment("# 标题\n- 列表项");

        // NODE 合并转发的单个节点。id-已有消息节点ID，content-该节点的消息内容，
        //      userId-发送者QQ号，nickname-发送者昵称，name-显示名，uin-发送者QQ号(字符串)
        MessageSegment node = new NodeSegment(
                null, Message.text("转发里的一条"), 10001L, "昵称", "显示名", "10001");

        // FORWARD 合并转发引用。id-转发消息资源ID
        MessageSegment forward = new ForwardSegment("forward-res-id");

        // POKE 戳一戳。userId-被戳的QQ号，id-戳类型ID
        MessageSegment poke = new PokeSegment(10001L, 1);

        // DICE 骰子。result-点数
        MessageSegment dice = new DiceSegment("6");

        // RPS 猜拳。result-结果
        MessageSegment rps = new RpsSegment("1");

        // CONTACT 推荐联系人。contactType-类型(qq/group)，id-被推荐的QQ号或群号
        MessageSegment contact = new ContactSegment("qq", "10001");

        // SHAKE 窗口抖动，无参数
        MessageSegment shake = new ShakeSegment();

        // KEYBOARD 按钮键盘，结构见 buildKeyboard()
        MessageSegment keyboard = buildKeyboard();

        // UNKNOWN 框架无法识别的消息段。protocolType-协议原始类型名，data-原始数据
        // 一般只在接收时出现，不需要主动构造
        MessageSegment unknown = new UnknownSegment("some_new_type", "{}");

        // RawSegment 用指定的 type 直接塞原始数据，绕过强类型封装。
        // 当框架版本还没跟上协议新增字段时可用它兜底
        MessageSegment raw = new RawSegment(MessageSegmentType.TEXT, "原始数据");

        context.logger().info("已构造消息段数量：" + List.of(
                text, image, music, video, record, file, flashFile, at, atAll, reply,
                json, xml, face, mface, markdown, node, forward, poke, dice, rps,
                contact, shake, keyboard, unknown, raw).size());
    }

    /**
     * KEYBOARD 消息段的完整结构：
     * KeyboardSegment -> List&lt;KeyboardRow&gt; -> List&lt;KeyboardButton&gt; -> KeyboardRender + KeyboardAction
     */
    private KeyboardSegment buildKeyboard() {
        // render：按钮外观。label-按钮文字，visitedLabel-点击后文字，style-样式编号
        KeyboardRender render = new KeyboardRender("点我", "已点击", 1);

        // permission：谁能点。type-权限类型，roleIds-允许的身份组，userIds-允许的QQ号
        KeyboardPermission permission = new KeyboardPermission(0, List.of(), List.of("10001"));

        // action：点击行为。type-行为类型，permission-权限，unsupportedTips-不支持时的提示，
        //         data-回传数据，reply-是否作为回复发出，enter-是否直接发送
        KeyboardAction action = new KeyboardAction(2, permission, "当前版本不支持", "callback_data", false, true);

        // button：id-按钮唯一标识
        KeyboardButton button = new KeyboardButton("btn-1", render, action);

        // row：一行按钮
        KeyboardRow row = new KeyboardRow(List.of(button));

        return new KeyboardSegment(List.of(row));
    }

    /**
     * 统一的异常处理。ApiException 是框架 API 异常的基类，其子类含义：
     * ApiRequestException          请求被拒绝，code() 是协议返回码
     * ApiPermissionDeniedException 权限不足
     * ApiTimeoutException          超时
     * ApiUnavailableException      服务不可用（如 Bot 掉线）
     * ApiProtocolException         协议解析失败
     */
    private <T> T logFailure(Throwable error) {
        Throwable cause = error instanceof java.util.concurrent.CompletionException ? error.getCause() : error;
        if (cause instanceof ApiException) {
            context.logger().warn("API 调用失败：" + cause.getMessage());
        } else {
            context.logger().error("未预期的异常", cause);
        }
        return null;
    }
}
