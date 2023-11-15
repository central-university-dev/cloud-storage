package cloud.storage.server;

import cloud.storage.data.Cmd;
import cloud.storage.data.Packet;
import cloud.storage.data.Payload;
import cloud.storage.nio.PayloadHandler;
import cloud.storage.server.file.manager.FileManager;
import cloud.storage.util.Pair;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketAddress;
import java.nio.file.Path;

public class DownloadHandler implements PayloadHandler {
    private final FileManager fileManager;

    public DownloadHandler(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    @Override
    public void handle(ChannelHandlerContext context, Payload payload) {
        String pathString = new String(payload.cmdBody);
        Path path = Path.of(pathString).normalize();
        SocketAddress address = context.channel().remoteAddress();
        Pair<InputStream, String> response = fileManager.downloadFile(address, path);
        if (response.getFirst() == null) {
            context.writeAndFlush(new Packet(new Payload(Cmd.MESSAGE, response.getSecond().getBytes())));
        } else {
            try (InputStream inputStream = response.getFirst()) {
                context.writeAndFlush(new Packet(new Payload(Cmd.DOWNLOAD, inputStream.readAllBytes())));
            } catch (IOException e) {
                System.err.println("Error occurred while trying do read file to send it to the client: " + e.getMessage());
                e.printStackTrace();
                // TODO :: add message handle
                context.writeAndFlush(new Packet(new Payload(Cmd.MESSAGE, "Failed to download file from cloud.".getBytes())));
            }
        }
    }
}
