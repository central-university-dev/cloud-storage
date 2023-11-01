package cloud.storage.nio;

import cloud.storage.data.Cmd;
import cloud.storage.util.Pair;

import java.util.List;

public class CommandMessage extends Pair<Cmd, List<String>> {
    public CommandMessage(Cmd cmd, List<String> args) {
        super(cmd, args);
    }
}
