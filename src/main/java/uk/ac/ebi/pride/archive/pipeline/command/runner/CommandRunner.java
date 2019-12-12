package uk.ac.ebi.pride.archive.pipeline.command.runner;

import java.util.Collection;

/**
 * CommandRunner provides an interface for certain type of command defined by generics
 *
 * @author Rui Wang
 * @version $Id$
 */
public interface CommandRunner {

    /**
     * Run a given command
     *
     * @param command given command
     * @throws CommandRunnerException   exception while executing the given command
     */
    void run(Collection<String> command) throws CommandRunnerException;
}
