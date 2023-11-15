package cloud.storage.nio;

import cloud.storage.data.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Default {@link MessageToByteEncoder} to encode outbound packets in bytes
 */
public class PacketEncoder extends MessageToByteEncoder<Packet> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Packet msg, ByteBuf out) {
        out.writeBytes(msg.getBytes());
    }

}
