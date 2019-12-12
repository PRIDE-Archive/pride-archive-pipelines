package uk.ac.ebi.pride.archive.pipeline.command.handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Default command result handler throws an IllegalStateException when exit value
 * is not zero
 *
 * @author Rui Wang
 * @version $Id$
 */
public class DefaultCommandResultHandler implements CommandResultHandler {

    @Override
    public void handle(Process p) {
        if (p.exitValue() != 0) {
            InputStream errInputStream = p.getErrorStream();
            String errorMessage = getErrorMessage(errInputStream);
            throw new CommandExecutionException(errorMessage);
        }
    }

    private String getErrorMessage(InputStream errInputStream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(errInputStream));
        String errorMessage = "";

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                errorMessage+=line;
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        return errorMessage;
    }
}
