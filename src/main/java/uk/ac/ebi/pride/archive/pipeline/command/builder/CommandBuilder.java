package uk.ac.ebi.pride.archive.pipeline.command.builder;

import java.util.Collection;

/**
 * Command builder interface for building command
 *
 * @author Rui Wang
 * @version $Id$
 */
public interface CommandBuilder<B extends CommandBuilder> {

    /**
     * Append a given argument to the command
     * @param argument  command argument
     * @return  this
     */
    B argument(String argument);

    /**
     * Append an given anchor and a given argument as pairs to the command
     * @param anchor    anchor e.g. -a --accession
     * @param argument  argument
     * @return this
     */
    B argument(String anchor, String argument);

    /**
     * Get command
     * @return  a list of strings representing the command
     */
    Collection<String> getCommand();
}
