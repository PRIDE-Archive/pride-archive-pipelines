package uk.ac.ebi.pride.archive.pipeline.jobs.projects;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import uk.ac.ebi.pride.archive.dataprovider.common.Tuple;
import uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType;
import uk.ac.ebi.pride.archive.pipeline.configuration.DataSourceConfiguration;
import uk.ac.ebi.pride.archive.pipeline.configuration.RepoConfig;
import uk.ac.ebi.pride.archive.pipeline.core.transformers.PrideProjectTransformer;
import uk.ac.ebi.pride.archive.pipeline.jobs.AbstractArchiveJob;
import uk.ac.ebi.pride.archive.pipeline.utility.SubmissionPipelineConstants;
import uk.ac.ebi.pride.archive.repo.client.FileRepoClient;
import uk.ac.ebi.pride.archive.repo.client.ProjectRepoClient;
import uk.ac.ebi.pride.archive.repo.models.file.ProjectFile;
import uk.ac.ebi.pride.archive.repo.models.project.Project;
import uk.ac.ebi.pride.mongodb.archive.model.files.MongoPrideFile;
import uk.ac.ebi.pride.mongodb.archive.model.msrun.MongoPrideMSRun;
import uk.ac.ebi.pride.mongodb.archive.model.projects.MongoPrideProject;
import uk.ac.ebi.pride.mongodb.archive.service.files.PrideFileMongoService;
import uk.ac.ebi.pride.mongodb.archive.service.projects.PrideProjectMongoService;
import uk.ac.ebi.pride.mongodb.configs.ArchiveMongoConfig;
import uk.ac.ebi.pride.solr.api.client.SolrProjectClient;
import uk.ac.ebi.pride.solr.commons.PrideSolrProject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * This Job takes the SDRF Data from the OracleDB and Sync into MongoDB and Solr.
 * <p>
 * Todo: We need to check what happen in case of Transaction error.
 *
 * @author ypriverol
 */
@Configuration
@Slf4j
//@EnableBatchProcessing
@Import({RepoConfig.class, ArchiveMongoConfig.class, DataSourceConfiguration.class})
public class SyncSdrfOracleToMongoFilesJob extends AbstractArchiveJob {

    @Autowired
    PrideProjectMongoService prideProjectMongoService;

    @Autowired
    SolrProjectClient solrProjectClient;

    @Autowired
    PrideFileMongoService prideFileMongoService;

    @Autowired
    FileRepoClient fileRepoClient;

    @Autowired
    ProjectRepoClient projectRepoClient;

    @Value("${ftp.protocol.url}")
    private String ftpProtocol;

    @Value("${aspera.protocol.url}")
    private String asperaProtocol;

    @Value("${accession:#{null}}")
    @StepScope
    private String accession;

    List<String> projectsContainingSdrf = new ArrayList<>();

    private void doSdrfFileSync(String accession) {
        try {
            Optional<MongoPrideProject> mongoPrideProjectOptional = prideProjectMongoService.findByAccession(accession);
            if (!mongoPrideProjectOptional.isPresent()) {
                return;
            }
            MongoPrideProject mongoPrideProject = mongoPrideProjectOptional.get();
            Project oracleProject = projectRepoClient.findByAccession(mongoPrideProject.getAccession());
            List<ProjectFile> oracleFiles = fileRepoClient.findAllByProjectId(oracleProject.getId());
            oracleFiles = oracleFiles.stream()
                    .filter(oracleFile -> oracleFile.getFileType().equals(ProjectFileType.EXPERIMENTAL_DESIGN))
                    .collect(Collectors.toList());

            if (oracleFiles == null || oracleFiles.size() == 0) {
                return;
            }
            projectsContainingSdrf.add(accession);
            List<MongoPrideMSRun> msRunRawFiles = new ArrayList<>();
            List<Tuple<MongoPrideFile, MongoPrideFile>> status = prideFileMongoService.insertAllFilesAndMsRuns(PrideProjectTransformer.transformOracleFilesToMongoFiles(oracleFiles, msRunRawFiles, oracleProject, ftpProtocol, asperaProtocol), msRunRawFiles);
            log.info("Number of files has been inserted -- " + status.size());
            if (msRunRawFiles.size() > 0) {
                //to-do
                log.info("Number of MS Run files has been inserted -- " + msRunRawFiles.size());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IllegalStateException(e);
        }
    }

    /**
     * The Files will be mapped only for the Projects that has been already sync into MongoDB.
     *
     * @return Step
     */
    public Step syncSdrfFileInformationToMongoDBStep() {
        return stepBuilderFactory.get(SubmissionPipelineConstants.PrideArchiveStepNames.PRIDE_ARCHIVE_ORACLE_TO_MONGO_SYNC_SDRF_FILES.name())
                .tasklet((stepContribution, chunkContext) -> {
                    //String accession = chunkContext.getStepContext().getStepExecution().getJobExecution().getJobParameters().getString("accession");
                    if (accession != null) {
                        doSdrfFileSync(accession);
                    } else {
                        prideProjectMongoService.getAllProjectAccessions().forEach(this::doSdrfFileSync);
                    }
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    private void doSolrSync(MongoPrideProject mongoPrideProject) {
        PrideSolrProject solrProject = PrideProjectTransformer.transformProjectMongoToSolr(mongoPrideProject);
        List<MongoPrideFile> files = prideFileMongoService.findFilesByProjectAccession(mongoPrideProject.getAccession());
        Set<String> fileNames = files.stream().map(MongoPrideFile::getFileName).collect(Collectors.toSet());
        solrProject.setProjectFileNames(fileNames);
        PrideSolrProject status = null;
        try {
            status = solrProjectClient.upsert(solrProject);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new IllegalStateException(e);
        }
        log.info("[Solr] The project -- " + status.getAccession() + " has been inserted in SolrCloud");
    }


    Step syncMongoSdrfProjectToSolrStep() {
        return stepBuilderFactory
                .get("syncMongoSdrfProjectToSolrStep")
                .tasklet((stepContribution, chunkContext) -> {
                    if (accession != null) {
                        Optional<MongoPrideProject> mongoPrideProjectOptional = prideProjectMongoService.findByAccession(accession);
                        if (mongoPrideProjectOptional.isPresent()) {
                            MongoPrideProject mongoPrideProject = mongoPrideProjectOptional.get();
                            doSolrSync(mongoPrideProject);
                        }
                    } else {
                        prideProjectMongoService.findByMultipleAccessions(projectsContainingSdrf)
                                .forEach(this::doSolrSync);
                    }
                    return RepeatStatus.FINISHED;
                })
                .build();
    }


    /**
     * Defines the job to Sync all the sdrf files from OracleDB into MongoDB database.
     *
     * @return the calculatePrideArchiveDataUsage job
     */
    @Bean
    public Job syncSdrfOracleToMongoFilesJob() {
        return jobBuilderFactory
                .get(SubmissionPipelineConstants.PrideArchiveJobNames.PRIDE_ARCHIVE_SDRF_ORACLE_MONGODB_FILE_SYNC.getName())
                .start(syncSdrfFileInformationToMongoDBStep())
                .next(syncMongoSdrfProjectToSolrStep())
                .build();
    }

}
