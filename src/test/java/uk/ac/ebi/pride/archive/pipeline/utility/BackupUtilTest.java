package uk.ac.ebi.pride.archive.pipeline.utility;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.ac.ebi.pride.archive.pipeline.configuration.JobRunnerTestConfiguration;
import uk.ac.ebi.pride.archive.pipeline.jobs.stats.PrideArchiveSubmissionStatsJob;
import uk.ac.ebi.pride.mongodb.molecules.model.peptide.PrideMongoPeptideEvidence;
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

    @Value("${pride.data.backup.path}")
    String backupPath;

    @Test
    public void getPrideMongoProteinEvidenceFromBackupTest(){
        String projectAccession = "PXD010142";
        String assayAccession = "93465";
        PrideMongoProteinEvidence prideMongoProteinEvidence = BackupUtil.getPrideMongoProteinEvidenceFromBackup(projectAccession, assayAccession, backupPath);
        System.out.println(prideMongoProteinEvidence.toString());
        PrideMongoPeptideEvidence prideMongoPeptideEvidence = BackupUtil.getPrideMongoPeptideEvidenceFromBackup(projectAccession, assayAccession, backupPath);
        System.out.println(prideMongoPeptideEvidence.toString());

        assayAccession = "93466";
        prideMongoProteinEvidence = BackupUtil.getPrideMongoProteinEvidenceFromBackup(projectAccession, assayAccession, backupPath);
        System.out.println(prideMongoProteinEvidence.toString());
        prideMongoPeptideEvidence = BackupUtil.getPrideMongoPeptideEvidenceFromBackup(projectAccession, assayAccession, backupPath);
        System.out.println(prideMongoPeptideEvidence.toString());
    }

}