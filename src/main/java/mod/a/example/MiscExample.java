package mod.a.example;

import com.povech.api.event.EventTarget;
import com.povech.api.event.request.FriendRequestEvent;
import com.povech.api.event.request.GroupRequestEvent;
import com.povech.api.plugin.PluginContext;
import com.povech.api.result.media.OcrResult;
import com.povech.api.result.media.OcrText;
import com.povech.api.service.media.MediaService;
import com.povech.api.service.request.RequestService;

import java.net.URI;
import java.util.Objects;

/**
 * 官方分类：其他
 *
 * 这一类在 SDK 里对应两个服务：
 * MediaService   媒体处理，3 个方法（OCR、取图片直链、取语音直链）
 * RequestService 好友与加群请求处理，4 个方法
 *
 * 尚未提供专用封装的 action 可通过 LlbotExample 调用。
 */
public class MiscExample {
    private final PluginContext context;
    private final MediaService mediaService;
    private final RequestService requestService;
    private final SettingsExample settings;

    public MiscExample(PluginContext context, SettingsExample settings) {
        this.context = Objects.requireNonNull(context, "context");
        this.mediaService = context.media();
        this.requestService = context.requests();
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    // ================= MediaService 全部方法 =================

    /**
     * 对图片做 OCR 文字识别。
     *
     * @param image 图片标识。通常传收到的 ImageSegment 的 file() 或 url()；
     *              具体接受哪种取决于协议端，若一种取不到就换另一种试。
     */
    public void ocrImage(String image) {
        mediaService.ocrImage(image)
                .thenAccept(this::printOcr)
                .exceptionally(this::warn);
    }

    /**
     * 由图片文件标识换取可访问的直链。
     * 收到的 ImageSegment.url() 有时会过期，需要重新取时用这个。
     */
    public void getImageUrl(String file) {
        mediaService.getImageUrl(file)
                .thenAccept((URI url) -> context.logger().info("图片直链：" + url))
                .exceptionally(this::warn);
    }

    /**
     * 由语音文件标识换取直链。
     */
    public void getRecordUrl(String file) {
        mediaService.getRecordUrl(file)
                .thenAccept((URI url) -> context.logger().info("语音直链：" + url))
                .exceptionally(this::warn);
    }

    /**
     * OcrResult 有 2 个字段：text() 是拼好的全文，entries() 是逐条结果。
     * OcrText 有 2 个字段：text() 文本，confidence() 置信度（0 到 1）。
     */
    private void printOcr(OcrResult result) {
        context.logger().info("识别全文：" + result.text());
        for (OcrText entry : result.entries()) {
            context.logger().info(String.format("  %s (置信度 %.2f)", entry.text(), entry.confidence()));
        }
    }

    // ================= RequestService 全部方法 =================

    /**
     * 收到好友请求。flag() 是这条请求的凭据，同意或拒绝都要回传它。
     * comment() 是对方填的验证信息，source() 是来源渠道。
     */
    @EventTarget
    public void onFriendRequest(FriendRequestEvent event) {
        long timestamp = event.timestamp(); // 事件时间戳
        long botId = event.botId();         // 机器人QQ号
        long userId = event.userId();       // 申请人QQ号
        String comment = event.comment();   // 验证信息
        String flag = event.flag();         // 请求凭据，处理时必须回传
        String source = event.source();     // 来源

        context.logger().info(String.format("好友请求 来自=%d 验证信息=%s 来源=%s bot=%d 时间=%d",
                userId, comment, source, botId, timestamp));

        if (settings.autoApproveFriendRequest()) {
            approveFriendRequest(flag, "通过插件自动添加");
        }
        context.logger().info("  请求凭据 flag=" + flag + "，处理时回传它");
    }

    /**
     * 同意好友请求。
     *
     * @param flag   来自 FriendRequestEvent.flag()
     * @param remark 通过后给对方设置的备注。
     *               参数名依据 OneBot11 的 set_friend_add_request 推断，SDK 未保留参数名。
     */
    public void approveFriendRequest(String flag, String remark) {
        requestService.approveFriendRequest(flag, remark)
                .thenRun(() -> context.logger().info("已同意好友请求"))
                .exceptionally(this::warn);
    }

    /**
     * 拒绝好友请求。
     *
     * @param flag   来自 FriendRequestEvent.flag()
     * @param reason 当前 Core 会把此参数填入 set_friend_add_request 的 remark 字段；
     *               LLBOT 对拒绝场景是否使用该字段由协议端决定。
     */
    public void rejectFriendRequest(String flag, String reason) {
        requestService.rejectFriendRequest(flag, reason)
                .thenRun(() -> context.logger().info("已拒绝好友请求"))
                .exceptionally(this::warn);
    }

    /**
     * 收到加群请求。注意 subType() 区分是"主动申请"还是"被邀请"，
     * 处理时必须把它一起回传，否则协议端不知道该处理哪种。
     */
    @EventTarget
    public void onGroupRequest(GroupRequestEvent event) {
        long timestamp = event.timestamp();     // 事件时间戳
        long botId = event.botId();             // 机器人QQ号
        long groupId = event.groupId();         // 目标群号
        long userId = event.userId();           // 申请人QQ号
        long inviterId = event.inviterId();     // 邀请人QQ号，主动申请时通常为 0
        long sourceGroupId = event.sourceGroupId(); // 来源群号
        String subType = event.subType();       // 请求子类型，如 add / invite
        String comment = event.comment();       // 验证信息
        String flag = event.flag();             // 请求凭据

        context.logger().info(String.format(
                "加群请求 群=%d 申请人=%d 邀请人=%d 来源群=%d 子类型=%s 验证=%s bot=%d 时间=%d",
                groupId, userId, inviterId, sourceGroupId, subType, comment, botId, timestamp));

        // 同上，默认不自动处理。subType 必须原样回传，否则协议端不知道处理哪种请求。
        //
        // approveGroupRequest(flag, subType);
        context.logger().info("  请求凭据 flag=" + flag + " subType=" + subType);
    }

    /**
     * 同意加群请求。
     *
     * @param flag    来自 GroupRequestEvent.flag()
     * @param subType 来自 GroupRequestEvent.subType()，原样回传即可
     */
    public void approveGroupRequest(String flag, String subType) {
        requestService.approveGroupRequest(flag, subType)
                .thenRun(() -> context.logger().info("已同意加群请求"))
                .exceptionally(this::warn);
    }

    /**
     * 拒绝加群请求。比同意多一个参数。
     *
     * @param flag    来自 GroupRequestEvent.flag()
     * @param subType 来自 GroupRequestEvent.subType()
     * @param reason  拒绝理由，映射到 set_group_add_request 的 reason 字段。
     */
    public void rejectGroupRequest(String flag, String subType, String reason) {
        requestService.rejectGroupRequest(flag, subType, reason)
                .thenRun(() -> context.logger().info("已拒绝加群请求"))
                .exceptionally(this::warn);
    }

    private <T> T warn(Throwable error) {
        context.logger().warn("其他接口调用失败：" + error.getMessage());
        return null;
    }
}
