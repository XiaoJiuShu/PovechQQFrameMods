package mod.a.example;

import com.povech.api.model.file.GroupFile;
import com.povech.api.model.file.GroupFolder;
import com.povech.api.plugin.PluginContext;
import com.povech.api.result.file.GroupFileListing;
import com.povech.api.service.file.FileService;

import java.net.URI;
import java.util.Objects;

/**
 * 官方分类：文件
 *
 * 覆盖 FileService 的全部 5 个方法。
 */
public class FileExample {
    private final PluginContext context;
    private final FileService fileService;

    public FileExample(PluginContext context) {
        this.context = Objects.requireNonNull(context, "context");
        this.fileService = context.files();
    }

    // ================= FileService 全部方法 =================

    /**
     * 列出群文件根目录。返回的 GroupFileListing 同时包含文件与子文件夹。
     */
    public void listRootFiles(long groupId) {
        fileService.listRootFiles(groupId)
                .thenAccept(listing -> printListing(groupId, "根目录", listing))
                .exceptionally(this::warn);
    }

    /**
     * 列出指定文件夹内容。
     *
     * @param folderId 文件夹ID，取自 GroupFolder.folderId()
     */
    public void listFiles(long groupId, String folderId) {
        fileService.listFiles(groupId, folderId)
                .thenAccept(listing -> printListing(groupId, "文件夹 " + folderId, listing))
                .exceptionally(this::warn);
    }

    /**
     * 获取群文件下载直链。
     *
     * @param fileId 文件ID，取自 GroupFile.fileId()
     * @param busId  业务ID，取自 GroupFile.busId()。这两个参数必须来自同一个 GroupFile，
     *               不能自己拼，否则协议端取不到文件。
     */
    public void getGroupFileUrl(long groupId, String fileId, int busId) {
        fileService.getGroupFileUrl(groupId, fileId, busId)
                .thenAccept((URI url) -> context.logger().info("下载直链：" + url))
                .exceptionally(this::warn);
    }

    /**
     * 上传文件。fileReference 按 LLBOT 所在端可访问的文件引用填写；
     * 本地路径只在 Bot 与框架位于同一台机器或共享路径时有效。
     */
    public void uploadGroupFile(long groupId, String fileReference, String name,
                                String folderId) {
        fileService.uploadGroupFile(groupId, fileReference, name, folderId)
                .thenAccept(fileId -> context.logger().info("群文件已上传，fileId=" + fileId))
                .exceptionally(this::warn);
    }

    public void deleteGroupFile(long groupId, String fileId, int busId) {
        fileService.deleteGroupFile(groupId, fileId, busId)
                .thenRun(() -> context.logger().info("群文件已删除：" + fileId))
                .exceptionally(this::warn);
    }

    /**
     * 组合用法：遍历根目录，为每个文件取下载直链。
     * 注意这里用 thenCompose 串起两次异步调用，不要在回调里阻塞等待。
     */
    public void resolveAllRootFileUrls(long groupId) {
        fileService.listRootFiles(groupId)
                .thenAccept(listing -> {
                    for (GroupFile file : listing.files()) {
                        fileService.getGroupFileUrl(groupId, file.fileId(), file.busId())
                                .thenAccept(url -> context.logger().info(file.name() + " -> " + url))
                                .exceptionally(this::warn);
                    }
                })
                .exceptionally(this::warn);
    }

    /**
     * 递归遍历整个群文件树。
     * 只演示一层递归写法；目录很深时注意请求量，协议端可能限流。
     */
    public void walk(long groupId, String folderId, int depth) {
        var future = folderId == null
                ? fileService.listRootFiles(groupId)
                : fileService.listFiles(groupId, folderId);

        future.thenAccept(listing -> {
            String indent = "  ".repeat(depth);
            for (GroupFile file : listing.files()) {
                context.logger().info(indent + "[文件] " + file.name());
            }
            for (GroupFolder folder : listing.folders()) {
                context.logger().info(indent + "[目录] " + folder.name());
                walk(groupId, folder.folderId(), depth + 1);
            }
        }).exceptionally(this::warn);
    }

    // ================= 返回模型字段速查 =================

    private void printListing(long groupId, String where, GroupFileListing listing) {
        context.logger().info(String.format("群 %d %s：%d 个文件，%d 个文件夹",
                groupId, where, listing.files().size(), listing.folders().size()));
        listing.files().forEach(this::printFile);
        listing.folders().forEach(this::printFolder);
    }

    /**
     * GroupFile 全部 11 个字段。
     */
    private void printFile(GroupFile file) {
        long groupId = file.groupId();               // 群号
        String fileId = file.fileId();               // 文件ID，取直链时用
        String name = file.name();                   // 文件名
        int busId = file.busId();                    // 业务ID，取直链时用
        long size = file.size();                     // 文件大小，字节
        long uploadedAt = file.uploadedAt();         // 上传时间戳
        long expiresAt = file.expiresAt();           // 过期时间戳，群文件有有效期
        long modifiedAt = file.modifiedAt();         // 最后修改时间戳
        int downloadCount = file.downloadCount();    // 下载次数
        long uploaderId = file.uploaderId();         // 上传者QQ号
        String uploaderName = file.uploaderName();   // 上传者昵称

        context.logger().info(String.format(
                "文件 %s id=%s busId=%d 大小=%d 群=%d 上传=%d 过期=%d 修改=%d 下载=%d次 上传者=%s(%d)",
                name, fileId, busId, size, groupId,
                uploadedAt, expiresAt, modifiedAt, downloadCount, uploaderName, uploaderId));
    }

    /**
     * GroupFolder 全部 7 个字段。
     */
    private void printFolder(GroupFolder folder) {
        long groupId = folder.groupId();             // 群号
        String folderId = folder.folderId();         // 文件夹ID，列目录时用
        String name = folder.name();                 // 文件夹名
        long createdAt = folder.createdAt();         // 创建时间戳
        long creatorId = folder.creatorId();         // 创建者QQ号
        String creatorName = folder.creatorName();   // 创建者昵称
        int totalFileCount = folder.totalFileCount();// 文件夹内文件总数

        context.logger().info(String.format(
                "目录 %s id=%s 群=%d 创建=%d 创建者=%s(%d) 文件数=%d",
                name, folderId, groupId, createdAt, creatorName, creatorId, totalFileCount));
    }

    private <T> T warn(Throwable error) {
        context.logger().warn("文件操作失败：" + error.getMessage());
        return null;
    }
}
