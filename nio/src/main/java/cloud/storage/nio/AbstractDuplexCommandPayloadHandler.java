package cloud.storage.nio;

import cloud.storage.data.Cmd;
import cloud.storage.data.Payload;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.util.List;

@ChannelHandler.Sharable
public abstract class AbstractDuplexCommandPayloadHandler extends ChannelDuplexHandler implements CommandHandler, PayloadHandler {

    protected abstract Cmd getCmd();

    @Override
    public abstract void execute(ChannelHandlerContext context, List<String> arguments, ChannelPromise promise);

    @Override
    public final void handle(ChannelHandlerContext context, Payload payload) {
        if (payload.cmd == getCmd()) {
            handle0(context, payload.cmdBody);
        } else {
            context.fireChannelRead(payload);
        }
        removeFromPipeline(context);
    }

    private final void removeFromPipeline(ChannelHandlerContext context) {
        context.pipeline().remove(this);
    }

    protected abstract void handle0(ChannelHandlerContext context, byte[] cmdBody);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Payload payload) {
            handle(ctx, payload);
        }
    }
}
