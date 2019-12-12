package uk.ac.ebi.pride.archive.pipeline.command.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Default implementation of the command builder
 *
 * @author Rui Wang
 * @version $Id$
 */
public class DefaultCommandBuilder implements CommandBuilder<DefaultCommandBuilder> {
    private final List<String> command;

    public DefaultCommandBuilder() {
        this.command = new ArrayList<>();
    }

    @Override
    public DefaultCommandBuilder argument(String argument) {
        command.add(argument);
        return this;
    }

    @Override
    public DefaultCommandBuilder argument(String anchor, String argument) {
        command.add(anchor);
        command.add(argument);
        return this;
    }

    @Override
    public Collection<String> getCommand() {
        return Collections.unmodifiableCollection(command);
    }
}
