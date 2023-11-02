package cloud.storage.client;

import cloud.storage.data.Cmd;
import cloud.storage.data.Packet;
import cloud.storage.data.Payload;
import cloud.storage.nio.TimeData;
import cloud.storage.nio.CommandHandler;
import cloud.storage.nio.PayloadHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.util.List;

/**
 * Client side handler of Time commands.
 */
class TimeHandler implements CommandHandler, PayloadHandler {
    private static final Cmd CMD = Cmd.TIME;

    private static Packet getPacket() {
        return new Packet(new Payload(CMD, null));
    }

    /**
     * Sends a request to server to get time on server-side.
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
     * Passes received time up the pipeline
     *
     * @param context context which got the payload.
     * @param cmdBody data of the payload to handle.
     */
    @Override
    public void handle(ChannelHandlerContext context, byte[] cmdBody) {
        context.fireChannelRead(TimeData.fromBytes(cmdBody).toString());
    }
}
