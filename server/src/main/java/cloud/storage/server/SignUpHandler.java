package cloud.storage.server;

import cloud.storage.data.Cmd;
import cloud.storage.data.Packet;
import cloud.storage.data.Payload;
import cloud.storage.server.file.manager.FileManager;
import cloud.storage.nio.PayloadHandler;
import cloud.storage.nio.SignInResponse;
import cloud.storage.nio.UserData;
import cloud.storage.util.Pair;
import io.netty.channel.ChannelHandlerContext;

import java.nio.ByteBuffer;

/**
 * Class for server side inbound signUp payloads handling.
 */
public class SignUpHandler implements PayloadHandler {
    private final FileManager fileManager;

    SignUpHandler(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    /**
     * Expects {@link UserData} in received payload.
     * Trying to sign up user in {@link FileManager} with passed {@link UserData}.
     * Sends {@link SignInResponse} with results of operation.
     *
     * @param context context which got the payload.
     * @param cmdBody data of the payload to handle.
     */
    @Override
    public void handle(ChannelHandlerContext context, byte[] cmdBody) {
        UserData userData = UserData.fromBytes(ByteBuffer.wrap(cmdBody));
        Pair<Boolean, String> result = fileManager.signUp(context.channel().remoteAddress(), userData);
        if (result.getFirst()) {
            context.writeAndFlush(new Packet(new Payload(Cmd.SIGN_IN,
                    SignInResponse.success(result.getSecond()).getBytes())));
        } else {
            context.writeAndFlush(new Packet(new Payload(Cmd.SIGN_UP,
                    result.getSecond().getBytes())));
        }
    }
}
