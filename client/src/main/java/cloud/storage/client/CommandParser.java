package cloud.storage.client;

import cloud.storage.data.Cmd;
import cloud.storage.nio.CommandMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import java.util.List;

public class CommandParser extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (msg instanceof String message) {
            List<String> words = List.of(message.strip().split("\\s+"));
            if (words.size() < 1) {
                promise.setFailure(new IllegalArgumentException("Wrong input. Please enter space-separated command and arguments"));
                return;
            }
            Cmd cmd;
            try {
                cmd = Cmd.getCmd(words.get(0));
            } catch (RuntimeException ignored) {
                promise.setFailure(new IllegalArgumentException("Unknown command."));
                return;
            }
            ctx.write(new CommandMessage(cmd, words.subList(1, words.size())), promise);

        } else {
            ctx.write(msg, promise);
        }
    }

}
