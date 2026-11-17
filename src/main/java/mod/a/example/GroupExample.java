package mod.a.example;

import com.povech.api.model.group.GroupInfo;
import com.povech.api.model.group.GroupMember;
import com.povech.api.model.group.GroupRole;
import com.povech.api.plugin.PluginContext;
import com.povech.api.service.group.GroupService;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * 官方分类：群组
 *
 * 覆盖 GroupService 的全部 9 个方法。
 */
public class GroupExample {
    private final PluginContext context;
    private final GroupService groupService;

    public GroupExample(PluginContext context) {
        this.context = Objects.requireNonNull(context, "context");
        this.groupService = context.groups();
    }

    // ================= 查询类 =================

    /**
     * 获取群资料。
     */
    public void getGroupInfo(long groupId) {
        groupService.getGroupInfo(groupId)
                .thenAccept(this::printGroupInfo)
                .exceptionally(this::warn);
    }

    /**
     * 获取单个群成员资料。
     *
     * @param groupId 群号
     * @param userId  群成员QQ号
     */
    public void getGroupMember(long groupId, long userId) {
        groupService.getGroupMember(groupId, userId)
                .thenAccept(this::printGroupMember)
                .exceptionally(this::warn);
    }

    /**
     * 获取全部群成员。大群返回的列表很长，注意不要整包打日志。
     */
    public void getGroupMembers(long groupId) {
        groupService.getGroupMembers(groupId)
                .thenAccept(members -> {
                    context.logger().info("群 " + groupId + " 成员数：" + members.size());
                    // 典型用法：筛出管理层
                    List<GroupMember> staff = members.stream()
                            .filter(m -> m.role() == GroupRole.OWNER || m.role() == GroupRole.ADMIN)
                            .toList();
                    context.logger().info("群主与管理员共 " + staff.size() + " 人");
                })
                .exceptionally(this::warn);
    }

    // ================= 管理类，需要机器人具备管理员权限 =================

    /**
     * 禁言群成员。权限不足时 future 以 ApiPermissionDeniedException 失败。
     *
     * @param duration 禁言时长。传 Duration.ZERO 为解除禁言（此语义来自 OneBot11 的
     *                 duration=0 约定，SDK 未在签名中标注，如有疑虑请核对框架实现）。
     */
    public void setGroupBan(long groupId, long userId, Duration duration) {
        groupService.setGroupBan(groupId, userId, duration)
                .thenRun(() -> context.logger().info(
                        duration.isZero()
                                ? "已解除禁言：" + userId
                                : "已禁言 " + userId + " " + duration.toMinutes() + " 分钟"))
                .exceptionally(this::warn);
    }

    /** 禁言 10 分钟 */
    public void banTenMinutes(long groupId, long userId) {
        setGroupBan(groupId, userId, Duration.ofMinutes(10));
    }

    /** 解除禁言 */
    public void liftBan(long groupId, long userId) {
        setGroupBan(groupId, userId, Duration.ZERO);
    }

    /**
     * 修改群成员名片。
     *
     * @param card 新名片。传空字符串通常表示清空名片，恢复显示昵称。
     */
    public void setGroupCard(long groupId, long userId, String card) {
        groupService.setGroupCard(groupId, userId, card)
                .thenRun(() -> context.logger().info("已修改 " + userId + " 的群名片为：" + card))
                .exceptionally(this::warn);
    }

    /**
     * 设置或取消管理员。只有群主能操作。
     *
     * @param admin true 设为管理员，false 取消管理员
     */
    public void setGroupAdmin(long groupId, long userId, boolean admin) {
        groupService.setGroupAdmin(groupId, userId, admin)
                .thenRun(() -> context.logger().info((admin ? "已设置管理员：" : "已取消管理员：") + userId))
                .exceptionally(this::warn);
    }

    public void kickMember(long groupId, long userId, boolean rejectAddRequest) {
        groupService.kickMember(groupId, userId, rejectAddRequest)
                .thenRun(() -> context.logger().info("已移出群成员：" + userId))
                .exceptionally(this::warn);
    }

    public void setGroupWholeBan(long groupId, boolean enabled) {
        groupService.setGroupWholeBan(groupId, enabled)
                .thenRun(() -> context.logger().info(enabled ? "已开启全体禁言" : "已解除全体禁言"))
                .exceptionally(this::warn);
    }

    public void sendGroupNotice(long groupId, String content, String image) {
        groupService.sendGroupNotice(groupId, content, image)
                .thenRun(() -> context.logger().info("群公告已发送：" + groupId))
                .exceptionally(this::warn);
    }

    // ================= 返回模型字段速查 =================

    /**
     * GroupInfo 全部 8 个字段。
     */
    private void printGroupInfo(GroupInfo group) {
        long groupId = group.groupId();                       // 群号
        String name = group.name();                           // 群名称
        String memo = group.memo();                           // 群备注
        long createdAt = group.createdAt();                   // 建群时间戳
        int memberCount = group.memberCount();                // 当前成员数
        int maximumMemberCount = group.maximumMemberCount();  // 群容量上限
        long ownerId = group.ownerId();                       // 群主QQ号
        String avatarUrl = group.avatarUrl();                 // 群头像URL

        context.logger().info(String.format(
                "群 %d 名称=%s 备注=%s 建群=%d 人数=%d/%d 群主=%d 头像=%s",
                groupId, name, memo, createdAt, memberCount, maximumMemberCount, ownerId, avatarUrl));
    }

    /**
     * GroupMember 全部 15 个字段。
     */
    private void printGroupMember(GroupMember member) {
        long groupId = member.groupId();                 // 群号
        long userId = member.userId();                   // 成员QQ号
        String nickname = member.nickname();             // QQ昵称
        String card = member.card();                     // 群名片，未设置时可能为空
        var sex = member.sex();                          // 性别，Sex 枚举
        int age = member.age();                          // 年龄
        String area = member.area();                     // 地区
        long joinedAt = member.joinedAt();               // 入群时间戳
        long lastMessageAt = member.lastMessageAt();     // 最后发言时间戳
        String level = member.level();                   // 群等级，注意是字符串
        GroupRole role = member.role();                  // 群身份，见 GroupRole
        boolean unfriendly = member.unfriendly();        // 是否有过不良记录
        String title = member.title();                   // 专属头衔
        long titleExpiresAt = member.titleExpiresAt();   // 头衔过期时间戳
        boolean cardChangeable = member.cardChangeable();// 是否允许修改群名片

        context.logger().info(String.format(
                "成员 %d@%d 昵称=%s 名片=%s 性别=%s 年龄=%d 地区=%s 入群=%d 末次发言=%d "
                        + "等级=%s 身份=%s 不良记录=%s 头衔=%s 头衔到期=%d 可改名片=%s",
                userId, groupId, nickname, card, sex, age, area, joinedAt, lastMessageAt,
                level, describeRole(role), unfriendly, title, titleExpiresAt, cardChangeable));
    }

    /**
     * GroupRole 枚举共 4 项。
     * 另有 GroupRole.fromProtocolValue(String) 把协议原始值（owner/admin/member）转枚举，
     * 一般由框架内部调用。
     *
     * 判断权限时记得把 UNKNOWN 当作无权限处理，不要用 != MEMBER 反推。
     */
    private String describeRole(GroupRole role) {
        return switch (role) {
            case OWNER -> "群主";
            case ADMIN -> "管理员";
            case MEMBER -> "普通成员";
            case UNKNOWN -> "未知";
        };
    }

    private <T> T warn(Throwable error) {
        context.logger().warn("群操作失败：" + error.getMessage());
        return null;
    }
}
