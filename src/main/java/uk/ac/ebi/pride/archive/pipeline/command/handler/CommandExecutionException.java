package uk.ac.ebi.pride.archive.pipeline.command.handler;

/**
 * Runtimme exception when there is an error while executing the command
 *
 * @author Rui Wang
 * @version $Id$
 */
public class CommandExecutionException extends RuntimeException {

    public CommandExecutionException() {
    }

    public CommandExecutionException(String message) {
        super(message);
    }

    public CommandExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommandExecutionException(Throwable cause) {
        super(cause);
    }
}
