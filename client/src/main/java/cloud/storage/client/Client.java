package cloud.storage.client;

import cloud.storage.nio.PacketEncoder;
import cloud.storage.nio.PayloadDecoder;
import cloud.storage.nio.ReplayingPacketDecoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

public class Client {
    public static void main(String[] args) throws Exception {
        String host = args[0];
        int port = Integer.parseInt(args[1]);

        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try (Reader reader = new InputStreamReader(System.in, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(reader);
             Writer writer = new OutputStreamWriter(System.out, StandardCharsets.UTF_8);
             BufferedWriter bufferedWriter = new BufferedWriter(writer)
        ) {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) {
                    ChannelPipeline p = ch.pipeline();
                    p.addLast("ReplayingPacketDecoder", new ReplayingPacketDecoder());
                    p.addLast("PayloadDecoder", new PayloadDecoder());

                    p.addLast("PacketEncoder", new PacketEncoder());

                    ClientHandler clientHandler = new ClientHandler(bufferedReader, bufferedWriter);
                    p.addLast("inboundPacketHandler", new ChannelPayloadHandler(clientHandler));

                    p.addLast("outboundCommandHandler", new ChannelCommandHandler(clientHandler));
                    p.addLast("commandParser", new CommandParser());

                    p.addLast("userInteraction", clientHandler);
                }
            });

            ChannelFuture f = b.connect(host, port).sync();

            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

}
