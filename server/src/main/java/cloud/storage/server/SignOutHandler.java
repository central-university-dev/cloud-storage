package cloud.storage.server;

import cloud.storage.data.Cmd;
import cloud.storage.data.Packet;
import cloud.storage.data.Payload;
import cloud.storage.file.manager.FileManager;
import cloud.storage.nio.PayloadHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * Class for server side inbound signOut payloads handling.
 */
public class SignOutHandler implements PayloadHandler {
    private final FileManager fileManager;

    public SignOutHandler(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    /**
     * Signing out user from the {@link FileManager}
     *
     * @param context context which got the payload.
     * @param cmdBody data of the payload to handle.
     */
    @Override
    public void handle(ChannelHandlerContext context, byte[] cmdBody) {
        fileManager.signOut(context.channel().remoteAddress());
        context.writeAndFlush(new Packet(new Payload(Cmd.SIGN_OUT, null)));
    }
}
