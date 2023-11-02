package cloud.storage.client;

import cloud.storage.data.Cmd;
import cloud.storage.util.Pair;

import java.util.List;

/**
 * Data class containing information about parsed command and arguments.
 *
 * @see CommandParser
 */
class CommandMessage extends Pair<Cmd, List<String>> {
    CommandMessage(Cmd cmd, List<String> args) {
        super(cmd, args);
    }
}
