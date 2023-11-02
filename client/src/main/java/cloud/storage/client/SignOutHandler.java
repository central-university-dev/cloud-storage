package cloud.storage.client;

import cloud.storage.data.Cmd;
import cloud.storage.data.Packet;
import cloud.storage.data.Payload;
import cloud.storage.nio.CommandHandler;
import cloud.storage.nio.PayloadHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.util.List;

/**
 * Client side handler of SignOut commands.
 */
class SignOutHandler implements CommandHandler, PayloadHandler {
    private static final Cmd CMD = Cmd.SIGN_OUT;

    private final ClientHandler clientHandler;

    SignOutHandler(ClientHandler clientHandler) {
        this.clientHandler = clientHandler;
    }

    private static Packet getPacket() {
        return new Packet(new Payload(CMD, null));
    }

    /**
     * Sends request to the server to sign out.
     *
     * @param context   context in which to execute the command
     * @param arguments command arguments
     * @param promise   promise to monitor the execution status of the command
     */
    @Override
    public void execute(ChannelHandlerContext context, List<String> arguments, ChannelPromise promise) {
        context.write(getPacket(), promise);
    }

    /**
     * Informs the {@link ClientHandler} about signing out.
     *
     * @param context context which got the payload.
     * @param cmdBody data of the payload to handle.
     */
    @Override
    public void handle(ChannelHandlerContext context, byte[] cmdBody) {
        clientHandler.signOut();
        context.fireChannelRead("Signed out");
    }
}
