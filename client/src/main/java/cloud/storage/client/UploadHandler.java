package cloud.storage.client;

import cloud.storage.data.Cmd;
import cloud.storage.data.Packet;
import cloud.storage.data.Payload;
import cloud.storage.nio.AbstractDuplexCommandPayloadHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;

public class UploadHandler extends AbstractDuplexCommandPayloadHandler {
    private static final Cmd CMD = Cmd.UPLOAD;

    private static Packet getPacket(byte[] cmdBody) {
        return new Packet(new Payload(CMD, cmdBody));
    }

    @Override
    protected Cmd getCmd() {
        return CMD;
    }

    @Override
    public void execute(ChannelHandlerContext context, List<String> arguments, ChannelPromise promise) {
        if (arguments.size() != 2) {
            promise.setFailure(new RuntimeException("Wrong number of arguments.\n" +
                    "You have to pass only path to file and path to save file on server as arguments."));
            return;
        }
        String clientPathString = arguments.get(0);
        String cloudPathString = arguments.get(1);

        Path clientFilePath;
        try {
            clientFilePath = Path.of(clientPathString);
        } catch (InvalidPathException e) {
            promise.setFailure(new RuntimeException("Invalid path passed. Please check the path and try again.", e));
            return;
        }
        try {
            File file = new File(clientFilePath.toUri());
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                ByteBuf byteBuf = Unpooled.buffer(fileInputStream.available());
                byteBuf.writeInt(cloudPathString.length())
                        .writeBytes(cloudPathString.getBytes())
                        .writeBytes(fileInputStream, fileInputStream.available());
                context.writeAndFlush(getPacket(byteBuf.array()), promise);
            } catch (FileNotFoundException e) {
                promise.setFailure(new RuntimeException("File not found. Please check the path and try again.", e));
            } catch (IOException e) {
                promise.setFailure(new RuntimeException("Error occurred while reading a file.", e));
            }
        } catch (IllegalArgumentException | SecurityException e) {
            promise.setFailure(new RuntimeException("Failed to get a file by path.", e));
        }
    }

    @Override
    public void handle0(ChannelHandlerContext context, byte[] cmdBody) {
        context.fireChannelRead(new String(cmdBody));
    }
}
