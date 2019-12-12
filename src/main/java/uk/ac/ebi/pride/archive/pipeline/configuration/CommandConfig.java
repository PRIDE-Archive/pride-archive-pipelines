package uk.ac.ebi.pride.archive.pipeline.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.ebi.pride.archive.pipeline.command.handler.DefaultCommandResultHandler;
import uk.ac.ebi.pride.archive.pipeline.command.runner.DefaultCommandRunner;


/**
 * @author Suresh Hewapathirana
 */
@Configuration
public class CommandConfig {

    @Bean
    DefaultCommandResultHandler commandResultHandler(){
        return new DefaultCommandResultHandler();
    }

    @Bean
    public DefaultCommandRunner commandRunner(DefaultCommandResultHandler commandResultHandler){
        return new DefaultCommandRunner(commandResultHandler);
    }
}

