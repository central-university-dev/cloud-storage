package cloud.storage.nio;

import cloud.storage.data.Cmd;
import cloud.storage.data.Payload;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

/**
 * Default {@link MessageToMessageDecoder} to decode Packet from inbound {@link ByteBuf}
 */
public class PayloadDecoder extends MessageToMessageDecoder<ByteBuf> {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
        Cmd cmd = Cmd.readCmd(msg);
        byte[] cmdBody = null;
        if (msg.readableBytes() > 0) {
            cmdBody = new byte[msg.readableBytes()];
            msg.readBytes(cmdBody);
        }
        out.add(new Payload(cmd, cmdBody));
        msg.release();
    }
}
