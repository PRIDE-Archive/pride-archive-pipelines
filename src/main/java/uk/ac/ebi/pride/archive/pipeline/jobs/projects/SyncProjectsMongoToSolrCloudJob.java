package uk.ac.ebi.pride.archive.pipeline.jobs.projects;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import uk.ac.ebi.pride.archive.pipeline.configuration.DataSourceConfiguration;
import uk.ac.ebi.pride.archive.pipeline.configuration.SolrApiClientConfig;
import uk.ac.ebi.pride.archive.pipeline.core.transformers.PrideProjectTransformer;
import uk.ac.ebi.pride.archive.pipeline.jobs.AbstractArchiveJob;
import uk.ac.ebi.pride.archive.pipeline.utility.SubmissionPipelineConstants;
import uk.ac.ebi.pride.mongodb.archive.model.files.MongoPrideFile;
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
 * This code is licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * ==Overview==
 * <p>
 * This class Job sync all the projects from MongoDB to SolrCloud. The first approach would be to index the projects from the
 * MongoDB and then other jobs can be used to index the from Oracle.
 *
 * <p>
 * Created by ypriverol (ypriverol@gmail.com) on 13/06/2018.
 */
@Configuration
@Slf4j
@Import({ArchiveMongoConfig.class, SolrApiClientConfig.class, DataSourceConfiguration.class})
public class SyncProjectsMongoToSolrCloudJob extends AbstractArchiveJob {


    @Autowired
    PrideProjectMongoService prideProjectMongoService;

    @Autowired
    PrideFileMongoService prideFileMongoService;

    @Autowired
    SolrProjectClient solrProjectClient;

    @Value("${accession:#{null}}")
    private String accession;

    private static int countOfProjects = 0;

    private static List<PrideSolrProject> prideSolrProjects = new ArrayList<>();


    private void doProjectSync(MongoPrideProject mongoPrideProject) {
        PrideSolrProject solrProject = getPrideSolrProjectFromMongoProject(mongoPrideProject);
        PrideSolrProject status = null;
        try {
            status = solrProjectClient.save(solrProject);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new IllegalStateException(e);
        }
        log.info("The project -- " + status.getAccession() + " has been inserted in SolrCloud");
    }

//    private void doFilesSync(PrideSolrProject prideSolrProject){
//        List<MongoPrideFile> files = prideFileMongoService.findFilesByProjectAccession(prideSolrProject.getAccession());
//        Set<String> fileNames = files.stream().map(MongoPrideFile::getFileName).collect(Collectors.toSet());
//        prideSolrProject.setProjectFileNames(fileNames);
//        PrideSolrProject savedProject = solrProjectService.update(prideSolrProject);
//        log.info("The files for project -- " + savedProject.getAccession() + " have been inserted in SolrCloud");
//    }

    /**
     * This methods connects to the database read all the Oracle information for public
     * @return
     */
    @Bean
    Step syncProjectMongoDBToSolrCloudStep() {
        return stepBuilderFactory
                .get(SubmissionPipelineConstants.PrideArchiveStepNames.PRIDE_ARCHIVE_MONGO_TO_SOLR_SYNC.name())
                .tasklet((stepContribution, chunkContext) -> {
                    if (accession != null) {
                        Optional<MongoPrideProject> mongoPrideProjectOptional = prideProjectMongoService.findByAccession(accession);
                        if (mongoPrideProjectOptional.isPresent()) {
                            MongoPrideProject mongoPrideProject = mongoPrideProjectOptional.get();
                            doProjectSync(mongoPrideProject);
                        }
                    } else {
                        prideProjectMongoService.findAllStream().forEach(this::doProjectSyncBatch);
                        if (prideSolrProjects.size() >= 1) {
                            saveBulkSolrProjects();
                        }
                    }
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    private void doProjectSyncBatch(MongoPrideProject mongoPrideProject) {
        PrideSolrProject solrProject = getPrideSolrProjectFromMongoProject(mongoPrideProject);
        prideSolrProjects.add(solrProject);
        countOfProjects++;
        if (prideSolrProjects.size() >= 1000) {
            saveBulkSolrProjects();
        }
    }

    private void saveBulkSolrProjects() {
        try {
            solrProjectClient.saveAll(prideSolrProjects);
            log.info(countOfProjects + " projects has been inserted in SolrCloud");
            prideSolrProjects.clear();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IllegalStateException(e);
        }
    }

    @NotNull
    private PrideSolrProject getPrideSolrProjectFromMongoProject(MongoPrideProject mongoPrideProject) {
        PrideSolrProject solrProject = PrideProjectTransformer.transformProjectMongoToSolr(mongoPrideProject);
        List<MongoPrideFile> files = prideFileMongoService.findFilesByProjectAccession(mongoPrideProject.getAccession());
        Set<String> fileNames = files.stream().map(MongoPrideFile::getFileName).collect(Collectors.toSet());
        solrProject.setProjectFileNames(fileNames);
        return solrProject;
    }

    /**
     * Clean all the documents in the SolrCloud Master for Sync
     * @return return Step
     */
    @Bean
    Step cleanSolrCloudStep() {
        return stepBuilderFactory
                .get(SubmissionPipelineConstants.PrideArchiveStepNames.PRIDE_ARCHIVE_ORACLE_CLEAN_SOLR.name())
                .tasklet((stepContribution, chunkContext) -> {
                    System.out.println("#####################Accession:" + accession);
                    if (accession != null) {
                        Optional<PrideSolrProject> prideSolrProject = solrProjectClient.findByAccession(accession);
                        if (prideSolrProject.isPresent()) {
                            String id = (String) prideSolrProject.get().getId();
                            solrProjectClient.deleteProjectById(id);
                            log.info("Document with id-accession: " + id + " - " + prideSolrProject.get().getAccession() + " has been deleted from the SolrCloud Master");
                        }
                    } else {
                            try {
                                solrProjectClient.deleteAll();
                            } catch (IOException e) {
                                log.error(e.getMessage(), e);
                                throw new IllegalStateException(e);
                            }
                        log.info("All Documents has been deleted from the SolrCloud Master");
                    }
                    return RepeatStatus.FINISHED;
                }).build();
    }

    /**
     * Sync the Files to Solr Project
     * @return Step
     */
//    @Bean
//    Step syncFilesToSolrProjectStep() {
//        return stepBuilderFactory
//                .get(SubmissionPipelineConstants.PrideArchiveStepNames.PRIDE_ARCHIVE_SYNC_FILES_TO_PROJECT_SOLR.name())
//                .tasklet((stepContribution, chunkContext) -> {
//                    if(accession != null){
//                        PrideSolrProject prideSolrProject = solrProjectService.findByAccession(accession);
//                        doFilesSync(prideSolrProject);
//                    }else{
//                        solrProjectService.findAll().forEach(this::doFilesSync);
//                    }
//                    return RepeatStatus.FINISHED;
//                }).build();
//    }


    /**
     * Defines the job to Sync all the projects from OracleDB into MongoDB database.
     *
     * @return the calculatePrideArchiveDataUsage job
     */
    @Bean
    public Job syncMongoProjectToSolrCloudJob() {
        return jobBuilderFactory
                .get(SubmissionPipelineConstants.PrideArchiveJobNames.PRIDE_ARCHIVE_MONGODB_SOLRCLOUD_SYNC.getName())
                .start(cleanSolrCloudStep())
                .next(syncProjectMongoDBToSolrCloudStep())
//                .next(syncFilesToSolrProjectStep()) //file sync is also included in above step
                .build();

    }



}
