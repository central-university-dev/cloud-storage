package cloud.storage.server;

import cloud.storage.data.Cmd;
import cloud.storage.data.Packet;
import cloud.storage.data.Payload;
import cloud.storage.data.SignInResponse;
import cloud.storage.data.UserData;
import cloud.storage.file.manager.FileManager;
import cloud.storage.nio.PayloadHandler;
import cloud.storage.util.Pair;
import io.netty.channel.ChannelHandlerContext;

import java.nio.ByteBuffer;

public class SignInHandler implements PayloadHandler {
    private static final Cmd CMD = Cmd.SIGN_IN;
    private final FileManager fileManager;

    SignInHandler(FileManager fileManager) {
        this.fileManager = fileManager;
    }


    private static Packet getPacket(byte[] cmdBody) {
        return new Packet(new Payload(CMD, cmdBody));
    }

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
