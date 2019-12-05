package uk.ac.ebi.pride.archive.pipeline.jobs.projects;


import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.pride.archive.pipeline.configuration.ArchiveOracleConfig;
import uk.ac.ebi.pride.archive.pipeline.jobs.AbstractArchiveJob;
import uk.ac.ebi.pride.archive.pipeline.utility.SubmissionPipelineConstants;
import uk.ac.ebi.pride.archive.repo.repos.project.Project;
import uk.ac.ebi.pride.archive.repo.repos.project.ProjectRepository;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author Suresh Hewapathirana
 */
@Configuration
@Slf4j
@PropertySource("classpath:application.properties")
@Import({ArchiveOracleConfig.class})
public class SyncMissingProjectsWithPCJob extends AbstractArchiveJob {

    @Autowired
    ProjectRepository oracleProjectRepository;

    /**
     * Defines the job to Sync all missing public projects from OracleDB into ProteomeXchange database.
     *
     * @return the  job
     */
    @Bean
    public Job syncMissingProjectsOracleToPCJob() {
        return jobBuilderFactory
                .get(SubmissionPipelineConstants.PrideArchiveJobNames.PRIDE_ARCHIVE_SYNC_MISSING_PROJECTS_ORACLE_PC.getName())
                .start(syncMissingProjectsOracleToPC())
                .build();
    }

    @Bean
    Step syncMissingProjectsOracleToPC() {
        return stepBuilderFactory
                .get(SubmissionPipelineConstants.PrideArchiveStepNames.PRIDE_ARCHIVE_MISSING_PROJ_ORACLE_TO_PC_SYNC.name())
                .tasklet(
                        (stepContribution, chunkContext) -> {

                            Set<String> pcPublicAccessions = getProteomXchangeData();
                            Set<String> oraclePublicAccessions = getOracleProjectAccessions();

                            for (String prideAccession : oraclePublicAccessions) {
                                if(!pcPublicAccessions.contains(prideAccession)){
                                    System.out.println( prideAccession + " is not public on ProteomeXchange");
                                }
                            }
                            return RepeatStatus.FINISHED;
                        })
                .build();
    }

    private Set<String> getProteomXchangeData() {

        final String URI = "http://proteomecentral.proteomexchange.org/cgi/GetDataset?outputMode=json";

        Set<String> accessions = new HashSet<>();

        RestTemplate restTemplate = new RestTemplate();
        String records = restTemplate.getForObject(URI, String.class);
        String[] recordsTemp = records.split("GetDataset\\?ID=");
        for (int i = 1; i < recordsTemp.length; i++) {
            String accession = recordsTemp[i].split("\" target")[0];
            if (accession.startsWith("PXD")) {
                accessions.add(accession.substring(0, 9));
            } else {
                accessions.add(accession.substring(0, 10));
            }
        }
        return accessions;
    }

    /**
     * Connect to Oracle Database and get project accessions of all the public projects
     * (project with old PRD accessions)
     *
     * @return Set of project accessions
     */
    private Set<String> getOracleProjectAccessions(){

        Iterable<Project> oracleAllProjects = oracleProjectRepository.findAll();
        Set<String> oracleAccessions = StreamSupport.stream(oracleAllProjects.spliterator(), false)
                .filter(Project::isPublicProject)
                .map(Project::getAccession)
                .collect(Collectors.toSet());

        System.out.println( "Number of Oracle projects: "+ oracleAccessions.size());
        return oracleAccessions;
    }

}