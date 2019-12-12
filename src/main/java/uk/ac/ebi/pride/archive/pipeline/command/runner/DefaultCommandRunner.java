package uk.ac.ebi.pride.archive.pipeline.command.runner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import uk.ac.ebi.pride.archive.pipeline.command.handler.CommandResultHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * String based command runner which the command represented as a string
 *
 * @author Rui Wang
 * @version $Id$
 */
@Slf4j
public class DefaultCommandRunner implements CommandRunner{
    private CommandResultHandler commandResultHandler;

    public DefaultCommandRunner(CommandResultHandler commandResultHandler) {
        Assert.notNull(commandResultHandler, "Command result handler cannot be null");

        this.commandResultHandler = commandResultHandler;
    }

    public void run(Collection<String> command) throws CommandRunnerException {
        Assert.notNull(command, "Input command cannot be null");

        try {
            executeCommand(command);
        } catch (InterruptedException e) {
            String msg = "Command interrupted: " + command;
            log.error(msg, e);
            throw new CommandRunnerException(msg, e);
        } catch (IOException e) {
            String msg = "Failed to start command: " + command;
            log.error(msg, e);
            throw new CommandRunnerException(msg, e);
        }
    }

    private void executeCommand(Collection<String> commands) throws IOException, InterruptedException {
        log.info("Executing Command:" + commands.toString());
        ProcessBuilder processBuilder = new ProcessBuilder(new ArrayList<>(commands));

        // run command
        Process process = processBuilder.start();
        process.waitFor();

        // handle command results
        commandResultHandler.handle(process);
    }
}
