package uk.ac.ebi.pride.archive.pipeline.command.handler;

/**
 * CommandResultHandler provides a strategy to handle command results
 *
 * @author Rui Wang
 * @version $Id$
 */
public interface CommandResultHandler {

    /**
     * Handle the result of a process
     *
     * @param p given process
     */
    void handle(Process p);
}
