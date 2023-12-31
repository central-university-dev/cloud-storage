package cloud.storage.nio;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

/**
 * Default {@link ReplayingDecoder} responsible for collecting whole inbound {@link cloud.storage.data.Packet} bytes
 * to pass {@link cloud.storage.data.Payload} data up the pipeline
 */
public class ReplayingPacketDecoder extends ReplayingDecoder<Void> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        int length = in.readInt();
        ByteBuf payloadBytes = in.readBytes(length);
        payloadBytes.retain();
        out.add(payloadBytes);
    }
}
