package cloud.storage.client;

import cloud.storage.data.Cmd;
import cloud.storage.data.Packet;
import cloud.storage.data.Payload;
import cloud.storage.nio.AbstractDuplexCommandPayloadHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;

public class DownloadHandler extends AbstractDuplexCommandPayloadHandler {
    private final static Cmd CMD = Cmd.DOWNLOAD;

    private volatile Path currentSaveFilePath;

    @Override
    protected Cmd getCmd() {
        return CMD;
    }

    @Override
    public void execute(ChannelHandlerContext context, List<String> arguments, ChannelPromise promise) {
        if (arguments.size() != 2) {
            promise.setFailure(new RuntimeException("Wrong number of arguments.\n" +
                    "You have to pass only path to file in cloud and path to save file as arguments."));
            return;
        }
        String cloudPathString = arguments.get(0);
        String clientPathString = arguments.get(1);

        try {
            currentSaveFilePath = Path.of(clientPathString);
        } catch (InvalidPathException e) {
            promise.setFailure(new RuntimeException("Invalid save path passed. Please check the path and try again.", e));
            return;
        }
        try {
            context.writeAndFlush(new Packet(new Payload(Cmd.DOWNLOAD, cloudPathString.getBytes())), promise);
        } catch (IllegalArgumentException | SecurityException e) {
            promise.setFailure(new RuntimeException("Failed to get a file by path.", e));
        }


    }

    @Override
    public void handle0(ChannelHandlerContext context, byte[] cmdBody) {
        if (currentSaveFilePath == null) {
            throw new RuntimeException("Got unwanted DOWNLOAD payload");
        }
        ByteBuf byteBuf = Unpooled.wrappedBuffer(cmdBody);

        try (ByteBufInputStream byteBufInputStream = new ByteBufInputStream(byteBuf, true)) {
            final Path unixHome = Path.of("~");
            if (currentSaveFilePath.startsWith(unixHome)) {
                currentSaveFilePath = Path.of(System.getProperty("user.home"))
                        .resolve(unixHome.relativize(currentSaveFilePath));
            }

            Path parentPath = currentSaveFilePath.getParent();
            if (parentPath != null) {
                new File(parentPath.toUri()).mkdirs();
            }


            Files.copy(byteBufInputStream, currentSaveFilePath);
            context.fireChannelRead("File saved to " + currentSaveFilePath);
            currentSaveFilePath = null;
        } catch (IOException e) {
            context.fireChannelRead("Failed to save file from cloud: " + e.getMessage());
        }
    }

}
