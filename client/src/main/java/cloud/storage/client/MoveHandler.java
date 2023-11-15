package cloud.storage.client;

import cloud.storage.data.Cmd;
import cloud.storage.data.Packet;
import cloud.storage.data.Payload;
import cloud.storage.nio.AbstractDuplexCommandPayloadHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.nio.ByteBuffer;
import java.util.List;

public class MoveHandler extends AbstractDuplexCommandPayloadHandler {
    private final static Cmd CMD = Cmd.MOVE;

    @Override
    protected Cmd getCmd() {
        return CMD;
    }

    @Override
    public void execute(ChannelHandlerContext context, List<String> arguments, ChannelPromise promise) {
        if (arguments.size() != 2) {
            promise.setFailure(new RuntimeException("Wrong number of arguments.\n" +
                    "You have to pass only path of file in cloud to move and new path of file to save as arguments."));
            return;
        }
        String cloudPathSourceString = arguments.get(0);
        String cloudPathDestString = arguments.get(1);

        //TODO:: remove copy-paste with string encoding
        try {
            byte[] cmdBody = new byte[Integer.BYTES + cloudPathSourceString.length() + Integer.BYTES + cloudPathDestString.length()];
            ByteBuffer.wrap(cmdBody)
                    .putInt(cloudPathSourceString.length())
                    .put(cloudPathSourceString.getBytes())
                    .putInt(cloudPathDestString.length())
                    .put(cloudPathDestString.getBytes());
            context.writeAndFlush(new Packet(new Payload(Cmd.MOVE, cmdBody)), promise);
        } catch (IllegalArgumentException | SecurityException e) {
            promise.setFailure(new RuntimeException("Failed to get a file by path.", e));
        }


    }

    @Override
    protected void handle0(ChannelHandlerContext context, byte[] cmdBody) {
    }
}
