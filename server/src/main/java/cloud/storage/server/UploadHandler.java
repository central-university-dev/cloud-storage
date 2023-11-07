package cloud.storage.server;

import cloud.storage.data.Cmd;
import cloud.storage.data.Packet;
import cloud.storage.data.Payload;
import cloud.storage.nio.PayloadHandler;
import cloud.storage.server.file.manager.FileManager;
import cloud.storage.util.Pair;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.file.Path;

public class UploadHandler implements PayloadHandler {
    private final static Cmd CMD = Cmd.UPLOAD;
    private final FileManager fileManager;

    public UploadHandler(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    private static Packet getPacket(byte[] cmdBody) {
        return new Packet(new Payload(CMD, cmdBody));
    }

    @Override
    public void handle(ChannelHandlerContext context, Payload payload) {
        ByteBuf byteBuf = Unpooled.wrappedBuffer(payload.cmdBody);
        int pathLength = byteBuf.readInt();
        byte[] pathBytes = new byte[pathLength];
        byteBuf.readBytes(pathBytes);
        String pathString = new String(pathBytes);
        // TODO:: handle InvalidPathException
        Path path = Path.of(pathString).normalize();
        SocketAddress address = context.channel().remoteAddress();
        try (ByteBufInputStream byteBufInputStream = new ByteBufInputStream(byteBuf, true)) {
            Pair<Boolean, String> result = fileManager.uploadFile(address, path, byteBufInputStream);
            if (result.getFirst()) {
                context.write(getPacket("File uploaded successfully.".getBytes()));
            } else {
                context.write(getPacket("Failed to upload file.".getBytes()));
            }
            if (result.getSecond() != null) {
                context.write(getPacket(result.getSecond().getBytes()));
            }
            context.flush();
        } catch (IOException e) {
            System.err.println("Error occurred during handling upload command: " + e.getMessage());
            context.write(getPacket("Failed to upload a file.".getBytes()));
        }
        context.flush();
    }
}
