package cloud.storage.client;

import cloud.storage.data.Cmd;
import cloud.storage.nio.CommandHandler;
import cloud.storage.data.Packet;
import cloud.storage.data.Payload;
import cloud.storage.nio.PayloadHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.util.List;

public class PingHandler implements CommandHandler, PayloadHandler {
    private static final Cmd CMD = Cmd.PING;

    private static Packet getPacket(byte[] cmdBody) {
        return new Packet(new Payload(CMD, cmdBody));
    }

    @Override
    public void execute(ChannelHandlerContext context, List<String> arguments, ChannelPromise promise) {
        context.write(getPacket(String.join(" ", arguments).getBytes()), promise);
    }

    @Override
    public void handle(ChannelHandlerContext context, byte[] cmdBody) {
        String message = cmdBody == null ? "" : new String(cmdBody);
        context.fireChannelRead("ping " + message);
    }
}
