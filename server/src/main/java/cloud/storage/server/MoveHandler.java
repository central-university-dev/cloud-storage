package cloud.storage.server;

import cloud.storage.data.Cmd;
import cloud.storage.data.Packet;
import cloud.storage.data.Payload;
import cloud.storage.nio.PayloadHandler;
import cloud.storage.server.file.manager.FileManager;
import cloud.storage.util.Pair;
import io.netty.channel.ChannelHandlerContext;

import java.nio.ByteBuffer;
import java.nio.file.Path;

public class MoveHandler implements PayloadHandler {
    private final FileManager fileManager;

    MoveHandler(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    @Override
    public void handle(ChannelHandlerContext context, Payload payload) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(payload.cmdBody);
        byte[] sourceBytes = new byte[byteBuffer.getInt()];
        byteBuffer.get(sourceBytes);
        String sourceString = new String(sourceBytes);
        byte[] destBytes = new byte[byteBuffer.getInt()];
        byteBuffer.get(destBytes);
        String destString = new String(destBytes);
        // TODO:: handle InvalidPathExceptions

        Path source = Path.of(sourceString);
        Path dest = Path.of(destString);

        Pair<Boolean, String> result = fileManager.moveFile(context.channel().remoteAddress(), source, dest);
        if (result.getFirst()) {
            context.writeAndFlush(new Packet(new Payload(Cmd.MESSAGE, "File moved successfully.".getBytes())));
        } else {
            context.writeAndFlush(new Packet(new Payload(Cmd.MESSAGE,
                    (result.getSecond() != null ? result.getSecond() : "Failed to move a file").getBytes())));
        }
    }
}
