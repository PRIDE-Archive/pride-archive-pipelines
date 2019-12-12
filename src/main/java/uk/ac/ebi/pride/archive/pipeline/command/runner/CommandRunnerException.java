package uk.ac.ebi.pride.archive.pipeline.command.runner;

/**
 * Runtime command runner execution exception
 *
 * @author Rui Wang
 * @version $Id$
 */
public class CommandRunnerException extends RuntimeException{

    public CommandRunnerException() {
    }

    public CommandRunnerException(String message) {
        super(message);
    }

    public CommandRunnerException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommandRunnerException(Throwable cause) {
        super(cause);
    }
}
