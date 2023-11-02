package cloud.storage.server;

import cloud.storage.data.Cmd;
import cloud.storage.data.Packet;
import cloud.storage.data.Payload;
import cloud.storage.file.manager.FileManager;
import cloud.storage.nio.PayloadHandler;
import cloud.storage.nio.SignInResponse;
import cloud.storage.nio.UserData;
import cloud.storage.util.Pair;
import io.netty.channel.ChannelHandlerContext;

import java.nio.ByteBuffer;

/**
 * Class for server side inbound signIn payloads handling.
 */
public class SignInHandler implements PayloadHandler {
    private static final Cmd CMD = Cmd.SIGN_IN;
    private final FileManager fileManager;

    SignInHandler(FileManager fileManager) {
        this.fileManager = fileManager;
    }


    private static Packet getPacket(byte[] cmdBody) {
        return new Packet(new Payload(CMD, cmdBody));
    }

    /**
     * Expects {@link UserData} in received payload.
     * Trying to sign in user in {@link FileManager} with passed {@link UserData}.
     * Sends {@link SignInResponse} with results of operation.
     *
     * @param context context which got the payload.
     * @param cmdBody data of the payload to handle.
     */
    @Override
    public void handle(ChannelHandlerContext context, byte[] cmdBody) {
        UserData userData = UserData.fromBytes(ByteBuffer.wrap(cmdBody));
        Pair<Boolean, String> result = fileManager.signIn(context.channel().remoteAddress(), userData);
        if (result.getFirst()) {
            context.writeAndFlush(getPacket(SignInResponse.success(userData).getBytes()));
        } else {
            context.writeAndFlush(getPacket(SignInResponse.failure(result.getSecond()).getBytes()));
        }
    }
}
