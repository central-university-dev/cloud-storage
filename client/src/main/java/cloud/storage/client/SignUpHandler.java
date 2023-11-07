package cloud.storage.client;

import cloud.storage.data.Cmd;
import cloud.storage.data.Packet;
import cloud.storage.data.Payload;
import cloud.storage.nio.AbstractDuplexCommandPayloadHandler;
import cloud.storage.nio.SignInResponse;
import cloud.storage.nio.UserData;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Client side handler of SignUp commands.
 */
class SignUpHandler extends AbstractDuplexCommandPayloadHandler {
    private static final Cmd CMD = Cmd.SIGN_UP;
    private final ClientHandler clientHandler;

    SignUpHandler(ClientHandler clientHandler) {
        this.clientHandler = clientHandler;
    }

    private static Packet getPacket(byte[] cmdBody) {
        return new Packet(new Payload(CMD, cmdBody));
    }

    @Override
    protected Cmd getCmd() {
        return CMD;
    }

    /**
     * Sends SignUp request to the server with {@link UserData}.
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
        context.writeAndFlush(getPacket(userData.getBytes()), promise);
    }

    /**
     * Passes received messages up the pipeline.
     *
     * @param context context which got the payload.
     * @param cmdBody data of the payload to handle.
     */
    @Override
    public void handle0(ChannelHandlerContext context, byte[] cmdBody) {
        SignInResponse signInResponse = SignInResponse.fromBytes(ByteBuffer.wrap(cmdBody));
        if (signInResponse.isSuccess()) {
            clientHandler.setWorkingDirectory(signInResponse.getMessage());
            context.fireChannelRead("Signed up successfully.");
        } else if (signInResponse.getMessage() != null) {
            context.fireChannelRead(signInResponse.getMessage());
        }
    }
}
