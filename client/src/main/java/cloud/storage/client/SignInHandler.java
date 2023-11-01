package cloud.storage.client;

import cloud.storage.data.Cmd;
import cloud.storage.data.Packet;
import cloud.storage.data.Payload;
import cloud.storage.data.SignInResponse;
import cloud.storage.data.UserData;
import cloud.storage.nio.CommandHandler;
import cloud.storage.nio.PayloadHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.nio.ByteBuffer;
import java.util.List;

public class SignInHandler implements CommandHandler, PayloadHandler {
    private static final Cmd CMD = Cmd.SIGN_IN;

    private final ClientHandler clientHandler;

    public SignInHandler(ClientHandler clientHandler) {
        this.clientHandler = clientHandler;
    }

    private static Packet getPacket(byte[] cmdBody) {
        return new Packet(new Payload(CMD, cmdBody));
    }

    @Override
    public void execute(ChannelHandlerContext context, List<String> arguments, ChannelPromise promise) {
        if (arguments.size() != 2) {
            promise.setFailure(new RuntimeException("Wrong number of arguments. You have to pass only login and password"));
            return;
        }
        UserData userData = new UserData(arguments.get(0), arguments.get(1));
        context.write(getPacket(userData.getBytes()), promise);
    }

    @Override
    public void handle(ChannelHandlerContext context, byte[] cmdBody) {
        SignInResponse signInResponse = SignInResponse.fromBytes(ByteBuffer.wrap(cmdBody));
        if (signInResponse.isSuccess()) {
            clientHandler.signIn(signInResponse.userData.getLogin());
            context.fireChannelRead("Signed in successfully");
        } else {
            context.fireChannelRead(signInResponse.message);
        }
    }
}