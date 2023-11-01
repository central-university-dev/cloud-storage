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

public class SignUpHandler implements PayloadHandler {
    private final FileManager fileManager;

    SignUpHandler(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    @Override
    public void handle(ChannelHandlerContext context, byte[] cmdBody) {
        UserData userData = UserData.fromBytes(ByteBuffer.wrap(cmdBody));
        Pair<Boolean, String> result = fileManager.signUp(context.channel().remoteAddress(), userData);
        if (result.getFirst()) {
            context.writeAndFlush(new Packet(new Payload(Cmd.SIGN_IN, SignInResponse.success(userData).getBytes())));
        } else {
            context.writeAndFlush(new Packet(new Payload(Cmd.SIGN_UP, result.getSecond().getBytes())));
        }
    }
}
