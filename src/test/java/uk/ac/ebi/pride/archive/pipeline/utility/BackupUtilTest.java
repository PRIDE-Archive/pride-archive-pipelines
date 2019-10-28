package uk.ac.ebi.pride.archive.pipeline.utility;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.ac.ebi.pride.archive.pipeline.configuration.JobRunnerTestConfiguration;
import uk.ac.ebi.pride.archive.pipeline.jobs.stats.PrideArchiveSubmissionStatsJob;
import uk.ac.ebi.pride.mongodb.molecules.model.protein.PrideMongoProteinEvidence;

import static org.junit.Assert.*;

/**
 * @author Suresh Hewapathirana
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {PrideArchiveSubmissionStatsJob.class, JobRunnerTestConfiguration.class})
@TestPropertySource(value = "classpath:application-test.properties")
@Slf4j
public class BackupUtilTest {

    @Test
    public void getPrideMongoProteinEvidenceFromBackupTest(){
        String projectAccession = "PXD010142";
        String assayAccession = "93465";
        PrideMongoProteinEvidence prideMongoProteinEvidence = BackupUtil.getPrideMongoProteinEvidenceFromBackup(projectAccession, assayAccession);
        System.out.println(prideMongoProteinEvidence.toString());
    }

}