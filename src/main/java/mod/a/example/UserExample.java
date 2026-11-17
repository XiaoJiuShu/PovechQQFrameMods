package mod.a.example;

import com.povech.api.model.user.FriendInfo;
import com.povech.api.model.user.Sex;
import com.povech.api.model.user.UserInfo;
import com.povech.api.plugin.PluginContext;
import com.povech.api.service.user.UserService;

import java.util.List;
import java.util.Objects;

/**
 * 官方分类：用户
 *
 * 覆盖 UserService 的全部 4 个方法。
 */
public class UserExample {
    private final PluginContext context;
    private final UserService userService;

    public UserExample(PluginContext context) {
        this.context = Objects.requireNonNull(context, "context");
        this.userService = context.users();
    }

    // ================= UserService 全部方法 =================

    /**
     * 获取用户资料，陌生人也可以查。
     *
     * @param userId  目标QQ号
     * @param noCache true 表示强制向协议端重新拉取，false 允许使用缓存。
     *                批量查询时建议传 false，避免频繁请求被限流。
     */
    public void getUserInfo(long userId, boolean noCache) {
        userService.getUserInfo(userId, noCache)
                .thenAccept(this::printUserInfo)
                .exceptionally(error -> {
                    context.logger().warn("获取用户资料失败：" + error.getMessage());
                    return null;
                });
    }

    /**
     * 获取机器人的好友列表。
     */
    public void getFriends() {
        userService.getFriends()
                .thenAccept(this::printFriends)
                .exceptionally(error -> {
                    context.logger().warn("获取好友列表失败：" + error.getMessage());
                    return null;
                });
    }

    public void sendLike(long userId, int times) {
        userService.sendLike(userId, times)
                .thenRun(() -> context.logger().info("已点赞 " + userId + "，次数=" + times))
                .exceptionally(error -> {
                    context.logger().warn("点赞失败：" + error.getMessage());
                    return null;
                });
    }

    /** fileReference 必须是 LLBOT 运行端可以读取的文件引用。 */
    public void setAvatar(String fileReference) {
        userService.setAvatar(fileReference)
                .thenRun(() -> context.logger().info("头像已更新"))
                .exceptionally(error -> {
                    context.logger().warn("设置头像失败：" + error.getMessage());
                    return null;
                });
    }

    // ================= 返回模型字段速查 =================

    /**
     * UserInfo 全部 12 个字段。
     */
    private void printUserInfo(UserInfo user) {
        long userId = user.userId();                 // QQ号
        String nickname = user.nickname();           // 昵称
        Sex sex = user.sex();                        // 性别，见 Sex 枚举
        int age = user.age();                        // 年龄
        String qid = user.qid();                     // QID（QQ 个性号）
        int level = user.level();                    // QQ 等级
        int loginDays = user.loginDays();            // 连续登录天数
        long registrationTime = user.registrationTime(); // 注册时间戳
        String signature = user.signature();         // 个性签名
        String city = user.city();                   // 城市
        String country = user.country();             // 国家
        String remark = user.remark();               // 机器人给该用户设置的备注

        context.logger().info(String.format(
                "用户 %d 昵称=%s 性别=%s 年龄=%d QID=%s 等级=%d 连登=%d天 注册=%d 签名=%s 地区=%s/%s 备注=%s",
                userId, nickname, describeSex(sex), age, qid, level,
                loginDays, registrationTime, signature, country, city, remark));
    }

    /**
     * FriendInfo 全部 3 个字段。
     */
    private void printFriends(List<FriendInfo> friends) {
        context.logger().info("好友总数：" + friends.size());
        for (FriendInfo friend : friends) {
            long userId = friend.userId();       // QQ号
            String nickname = friend.nickname(); // 对方昵称
            String remark = friend.remark();     // 机器人设置的备注，未设置时可能为空

            context.logger().info(String.format("好友 %d 昵称=%s 备注=%s", userId, nickname, remark));
        }
    }

    /**
     * Sex 枚举共 3 项：MALE、FEMALE、UNKNOWN。
     * 另有 Sex.fromProtocolValue(String) 用于把协议原始值（male/female）转成枚举，
     * 一般由框架内部调用，插件通常不需要。
     */
    private String describeSex(Sex sex) {
        return switch (sex) {
            case MALE -> "男";
            case FEMALE -> "女";
            case UNKNOWN -> "未知";
        };
    }
}
