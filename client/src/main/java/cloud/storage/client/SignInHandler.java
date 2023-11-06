package cloud.storage.client;

import cloud.storage.data.Cmd;
import cloud.storage.data.Packet;
import cloud.storage.data.Payload;
import cloud.storage.nio.CommandHandler;
import cloud.storage.nio.PayloadHandler;
import cloud.storage.nio.SignInResponse;
import cloud.storage.nio.UserData;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Client side handler of SignIn commands.
 */
class SignInHandler implements CommandHandler, PayloadHandler {
    private static final Cmd CMD = Cmd.SIGN_IN;

    private final ClientHandler clientHandler;

    SignInHandler(ClientHandler clientHandler) {
        this.clientHandler = clientHandler;
    }

    private static Packet getPacket(byte[] cmdBody) {
        return new Packet(new Payload(CMD, cmdBody));
    }

    /**
     * Sends SignIn request to the server with {@link UserData}.
     *
     * @param context   context in which to execute the command
     * @param arguments command arguments
     * @param promise   promise to monitor the execution status of the command
     */
    @Override
    public void execute(ChannelHandlerContext context, List<String> arguments, ChannelPromise promise) {
        if (arguments.size() != 2) {
            promise.setFailure(new RuntimeException("Wrong number of arguments. You have to pass only login and password"));
            return;
        }
        UserData userData = new UserData(arguments.get(0), arguments.get(1));
        context.write(getPacket(userData.getBytes()), promise);
    }

    /**
     * Handles {@link SignInResponse} from server and informs the {@link ClientHandler} about successful signing in.
     *
     * @param context context which got the payload.
     * @param cmdBody data of the payload to handle.
     */
    @Override
    public void handle(ChannelHandlerContext context, byte[] cmdBody) {
        SignInResponse signInResponse = SignInResponse.fromBytes(ByteBuffer.wrap(cmdBody));
        if (signInResponse.isSuccess()) {
            clientHandler.setWorkingDirectory(signInResponse.getMessage());
            context.fireChannelRead("Signed in successfully");
        } else {
            context.fireChannelRead(signInResponse.getMessage());
        }
    }
}