package uk.ac.ebi.pride.archive.pipeline.jobs.projects;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import uk.ac.ebi.pride.archive.dataprovider.common.Tuple;
import uk.ac.ebi.pride.archive.pipeline.configuration.ArchiveRedisConfig;
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
import java.util.*;
import java.util.stream.Collectors;


/**
 * This Job takes the Data from the OracleDB and Sync into MongoDB. A parameter is needed if the user wants to override the
 * existing projects in the database.
 * <p>
 * Todo: We need to check what happen in case of Transaction error.
 *
 * @author hewapathirana
 */
@Configuration
@Slf4j
@PropertySource("classpath:application.properties")
@Import({RepoConfig.class, ArchiveMongoConfig.class, DataSourceConfiguration.class, ArchiveRedisConfig.class})
public class SyncMissingProjectsWithMongoJob extends AbstractArchiveJob {


    @Autowired
    PrideProjectMongoService prideProjectMongoService;

    @Autowired
    ProjectRepoClient projectRepoClient;

    @Autowired
    FileRepoClient fileRepoClient;

    @Autowired
    PrideFileMongoService prideFileMongoService;

    @Autowired
    SolrProjectClient solrProjectClient;

    @Value("${ftp.protocol.url}")
    private String ftpProtocol;

    @Value("${aspera.protocol.url}")
    private String asperaProtocol;

    /**
     * Defines the job to Sync all missing projects from OracleDB into MongoDB database.
     *
     * @return the  job
     */
    @Bean
    public Job syncMissingProjectsOracleToMongoJob() {
        return jobBuilderFactory
                .get(SubmissionPipelineConstants.PrideArchiveJobNames.PRIDE_ARCHIVE_SYNC_MISSING_PROJECTS_ORACLE_MONGODB.getName())
                .start(syncMissingProjectOracleToMongoDB())
                .build();
    }

    /**
     * This methods connects to the database read all the Oracle information for public
     *
     * @return
     */
    @Bean
    Step syncMissingProjectOracleToMongoDB() {
        return stepBuilderFactory
                .get(SubmissionPipelineConstants.PrideArchiveStepNames.PRIDE_ARCHIVE_MISSING_PROJ_ORACLE_TO_MONGO_SYNC.name())
                .tasklet(
                        (stepContribution, chunkContext) -> {

                            final Set<String> oracleProjectAccessions = getOracleProjectAccessions();
                            final Set<String> mongoDBProjectAccessions = getMongoProjectAccessions();

                            log.info("Number of projects in Oracle DB: " + oracleProjectAccessions.size());
                            log.info("Number of projects in Mongo DB : " + mongoDBProjectAccessions.size());

                            Set<String> oracleProjectAccessionsMongoCopy = new HashSet<>(oracleProjectAccessions);
                            Set<String> mongoDBProjectAccessionsCopy = new HashSet<>(mongoDBProjectAccessions);

                            // get list of accessions missing in mongoDB
                            oracleProjectAccessionsMongoCopy.removeAll(mongoDBProjectAccessions);
                            log.info("List of accessions missing in MongoDB: " + oracleProjectAccessionsMongoCopy.toString());
                            oracleProjectAccessionsMongoCopy.forEach(p -> {
                                syncProjectToMongo(p);
                                syncProjectFilesToMongo(p);
                                syncProjectToSolr(p);
                            });

                            // get list of accessions missing in in Oracle due to reset or mistakenly added to Mongo
                            mongoDBProjectAccessionsCopy.removeAll(oracleProjectAccessions);
                            log.info("List of accessions mistakenly added to MongoDB: " + mongoDBProjectAccessionsCopy.toString());
                            for (String accession : mongoDBProjectAccessionsCopy) {
                                removeProjectFromMongo(accession);
                                removeProjectFromSolr(accession);
                            }
                            return RepeatStatus.FINISHED;
                        })
                .build();
    }

    /**
     * Connect to Oracle Database and get project accessions of all the public projects
     * (project with old PRD accessions)
     *
     * @return Set of project accessions
     */
    private Set<String> getOracleProjectAccessions() throws IOException {

        Set<String> allAccessions = new HashSet<>(projectRepoClient.getAllPublicAccessions());
        log.info("Number of Oracle projects: " + allAccessions.size());
        return allAccessions;
    }

    /**
     * Connect to MongoDB Database and get project accessions of all the projects
     *
     * @return Set of project accessions
     */
    private Set<String> getMongoProjectAccessions() {

        Set<String> mongoProjectAccessions = prideProjectMongoService.getAllProjectAccessions();
        log.info("Number of MongoDB projects: " + mongoProjectAccessions.size());
        return mongoProjectAccessions;
    }

    private void syncProjectToMongo(String accession) {
        try {
            Project oracleProject = projectRepoClient.findByAccession(accession);
            if (!oracleProject.isPublicProject()) {
                return;
            }
            MongoPrideProject mongoPrideProject = PrideProjectTransformer.transformOracleToMongo(oracleProject);
            Optional<MongoPrideProject> status = prideProjectMongoService.upsert(mongoPrideProject);
            log.info(oracleProject.getAccession() + "-- [Mongo] project inserted Status " + status.isPresent());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IllegalStateException(e);
        }
    }

    private void syncProjectFilesToMongo(String accession) {
        try {
            Optional<MongoPrideProject> mongoPrideProjectOptional = prideProjectMongoService.findByAccession(accession);
            if (!mongoPrideProjectOptional.isPresent()) {
                return;
            }
            MongoPrideProject mongoPrideProject = mongoPrideProjectOptional.get();
            Project oracleProject = projectRepoClient.findByAccession(mongoPrideProject.getAccession());
            List<ProjectFile> oracleFiles = fileRepoClient.findAllByProjectId(oracleProject.getId());

            List<MongoPrideMSRun> msRunRawFiles = new ArrayList<>();
            List<Tuple<MongoPrideFile, MongoPrideFile>> status = prideFileMongoService.insertAllFilesAndMsRuns(PrideProjectTransformer.transformOracleFilesToMongoFiles(oracleFiles, msRunRawFiles, oracleProject, ftpProtocol, asperaProtocol), msRunRawFiles);
            log.info("[Mongo] Number of files has been inserted -- " + status.size());
            if (msRunRawFiles.size() > 0) {
                //to-do
                log.info("[Mongo] Number of MS Run files has been inserted -- " + msRunRawFiles.size());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IllegalStateException(e);
        }
    }

    private void syncProjectToSolr(String accession) {
        if (accession == null) {
            return;
        }
        Optional<MongoPrideProject> mongoPrideProjectOptional = prideProjectMongoService.findByAccession(accession);
        if (!mongoPrideProjectOptional.isPresent()) {
            return;
        }
        MongoPrideProject mongoPrideProject = mongoPrideProjectOptional.get();
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

    private void removeProjectFromMongo(String accession) {
        prideProjectMongoService.deleteByAccession(accession);
        prideFileMongoService.deleteByAccession(accession);
    }

    private void removeProjectFromSolr(String accession) throws IOException {
        Optional<PrideSolrProject> prideSolrProject = solrProjectClient.findByAccession(accession);
        if (prideSolrProject.isPresent()) {
            String id = (String) prideSolrProject.get().getId();
            solrProjectClient.deleteProjectById(id);
            log.info("Document with id-accession: " + id + " - " + prideSolrProject.get().getAccession() + " has been deleted from the SolrCloud Master");
        }
    }
}
