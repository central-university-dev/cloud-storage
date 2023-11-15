package cloud.storage.client;

import cloud.storage.data.Cmd;
import cloud.storage.data.Packet;
import cloud.storage.data.Payload;
import cloud.storage.nio.AbstractDuplexCommandPayloadHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.util.List;

/**
 * Client side handler of Ping commands
 */
class PingHandler extends AbstractDuplexCommandPayloadHandler {
    private static final Cmd CMD = Cmd.PING;

    private static Packet getPacket(byte[] cmdBody) {
        return new Packet(new Payload(CMD, cmdBody));
    }

    @Override
    protected Cmd getCmd() {
        return CMD;
    }

    /**
     * Sends a ping packet to server.
     *
     * @param context   context in which to execute the command
     * @param arguments command arguments
     * @param promise   promise to monitor the execution status of the command
     */
    @Override
    public void execute(ChannelHandlerContext context, List<String> arguments, ChannelPromise promise) {
        context.writeAndFlush(getPacket(String.join(" ", arguments).getBytes()), promise);
    }

    /**
     * Gets the message of ping response.
     *
     * @param context context which got the payload.
     * @param cmdBody data of the payload to handle.
     */
    @Override
    public void handle0(ChannelHandlerContext context, byte[] cmdBody) {
        String message = cmdBody == null ? "" : new String(cmdBody);
        context.fireChannelRead("ping " + message);
    }
}
