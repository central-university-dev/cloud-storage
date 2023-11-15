package cloud.storage.client;

import cloud.storage.data.Cmd;
import cloud.storage.nio.AbstractDuplexCommandPayloadHandler;
import cloud.storage.util.Pair;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Primary handler on client-side application.
 * Responsible for user interaction: receiving commands and returning messages.
 *
 * @see ChannelCommandHandler
 * @see ChannelPayloadHandler
 */
class ClientHandler extends ChannelInboundHandlerAdapter {
    final static String COMMAND_HANDLER_NAME = "commandHandler";
    private final Map<Cmd, AbstractDuplexCommandPayloadHandler> HANDLER_INSTANCES;
    private final ExecutorService executorService;
    private volatile ChannelHandlerContext ctx;
    private volatile BufferedReader reader;
    private volatile BufferedWriter writer;
    private volatile String workingDirectory;

    /**
     * @param reader -- where to read user commands
     * @param writer -- where to print messages to user
     */
    ClientHandler(BufferedReader reader, BufferedWriter writer) {
        super();
        this.reader = reader;
        this.writer = writer;
        this.executorService = Executors.newFixedThreadPool(1);
        HANDLER_INSTANCES = Map.of(
                Cmd.PING, new PingHandler(),
                Cmd.TIME, new TimeHandler(),
                Cmd.SIGN_UP, new SignUpHandler(this),
                Cmd.SIGN_IN, new SignInHandler(this),
                Cmd.SIGN_OUT, new SignOutHandler(this),
                Cmd.UPLOAD, new UploadHandler(),
                Cmd.DOWNLOAD, new DownloadHandler(),
                Cmd.MOVE, new MoveHandler()
        );
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        this.ctx = ctx;
        startWorking();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        this.ctx = null;
        shutdown();
    }

    private void startWorking() {
        println("Connected to server.");
        sendCommand();
    }

    /**
     * Gets user command from passed reader and sends it down the pipeline.
     */
    private void sendCommand() {
        executorService.submit(() -> {
            print("<" + (workingDirectory == null ? "" : workingDirectory) + ">:");

            String input = readLine();
            if (input == null) {
                shutdown();
                return;
            }
            if (input.equals("exit")) {
                shutdown();
                return;
            }
            if (input.equals("help")) {
                printHelp();
                return;
            }

            ChannelPromise promise = ctx.channel().newPromise();
            executorService.submit(() -> {
                Pair<Cmd, List<String>> res = parseCommand(input, promise);
                if (res == null) {
                    return;
                }
                executeCommand(res.getFirst(), res.getSecond(), promise);
            });
            promise.addListener(sendNextCommand);
        });
    }

    private static Pair<Cmd, List<String>> parseCommand(String commandInput, ChannelPromise promise) {
        List<String> words = List.of(commandInput.strip().split("\\s+"));
        if (words.size() < 1) {
            promise.setFailure(new IllegalArgumentException("Wrong input. Please enter space-separated command and arguments"));
            return null;
        }
        Cmd cmd;
        try {
            cmd = Cmd.getCmd(words.get(0));
        } catch (RuntimeException ignored) {
            promise.setFailure(new IllegalArgumentException("Unknown command."));
            return null;
        }
        return new Pair<>(cmd, words.subList(1, words.size()));
    }

    private void executeCommand(Cmd cmd, List<String> arguments, ChannelPromise promise) {
        AbstractDuplexCommandPayloadHandler handler = HANDLER_INSTANCES.get(cmd);
        if (handler == null) {
            promise.setFailure(new RuntimeException("Unknown command passed."));
            return;
        }
        try {
            ctx.pipeline().addBefore(Client.CLIENT_HANDLER_NAME, COMMAND_HANDLER_NAME, handler);
            handler.execute(ctx, arguments, promise);
        } catch (RuntimeException e) {
            promise.setFailure(e);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof String string) {
            println(string);
            print("<" + (workingDirectory == null ? "" : workingDirectory) + ">:");
        }
    }

    private void printHelp() {
        println("<> means that you are not signed in yet.");
        println("<username> means your username.");
        println("Available commands:");
        println("ping words...");
        println("\tServer will reply with the same message");
        println("time");
        println("\tGet the server time");
        println("signUp login password");
        println("\tTry to sign up on the server with passed login and password.");
        println("signIn login password");
        println("\tTry to sign in on the server with passed login and password.");
        println("signOut");
        println("\tSign out from server.");
        println("exit");
        println("\tShutdown client");
        println("help");
        println("\tShow this message");
    }

    /**
     * Responsible to continue working at the end of the previous command.
     */
    private final ChannelFutureListener sendNextCommand = new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) {
            if (future.isCancelled()) {
                System.err.println("Command was cancelled");
                future.channel().pipeline().remove(COMMAND_HANDLER_NAME);
            } else if (future.cause() != null) {
                System.err.println("Command ended with failure: " + future.cause().getMessage());
                future.channel().pipeline().remove(COMMAND_HANDLER_NAME);
            }
            sendCommand();
        }
    };

    private void shutdown() {
        println("Shutting down");
        if (ctx != null && ctx.channel().isOpen()) {
            ctx.channel().close();
        }
        executorService.shutdownNow();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        shutdown();
    }

    private String readLine() {
        try {
            synchronized (reader) {
                return reader.readLine();
            }
        } catch (IOException e) {
            System.err.println("Error occurred while trying to read new line: " + e);
            System.err.println(Arrays.toString(e.getStackTrace()));
            shutdown();
            return null;
        }
    }

    private void print(String message) {
        if (writer == null) {
            return;
        }
        try {
            synchronized (writer) {
                writer.write(message);
                writer.flush();
            }
        } catch (IOException e) {
            System.err.println("Error occurred while trying to print a message: " + e);
            System.err.println(Arrays.toString(e.getStackTrace()));
            writer = null;
            shutdown();
        }
    }

    private void println(String message) {
        print(message);
        print(System.lineSeparator());
    }

    /**
     * Remembers the user's login which was used to authorize on the server
     *
     * @param workingDirectory - the user's login that used to authorize on the server
     */
    protected void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    /**
     * Forgets the user's login from server side.
     */
    protected void resetWorkingDirectory() {
        this.workingDirectory = null;
    }
}
