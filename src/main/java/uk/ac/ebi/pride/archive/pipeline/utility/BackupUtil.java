package uk.ac.ebi.pride.archive.pipeline.utility;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paranamer.ParanamerModule;
import uk.ac.ebi.pride.mongodb.molecules.model.peptide.PrideMongoPeptideEvidence;
import uk.ac.ebi.pride.mongodb.molecules.model.protein.PrideMongoProteinEvidence;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;

/**
 * @author Suresh Hewapathirana
 */
public class BackupUtil {

    private static final ObjectMapper objectMapper;
    private static final String JSON_EXT = ".json";

    static {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new ParanamerModule());
    }

    public static void backupPrideMongoProteinEvidence(PrideMongoProteinEvidence prideMongoProteinEvidence, String backupPath) throws Exception {
        backupPath = createBackupDir(backupPath, prideMongoProteinEvidence.getProjectAccession());
        String filename = prideMongoProteinEvidence.getProjectAccession() + "_" + prideMongoProteinEvidence.getAssayAccession() + "_" + PrideMongoProteinEvidence.class.getName() + JSON_EXT;
        String backupLocation = backupPath + File.separator + filename;
        objectMapper.writeValue(new File(backupLocation), prideMongoProteinEvidence);
    }

    public static void backupPrideMongoPeptideEvidence(PrideMongoPeptideEvidence prideMongoPeptideEvidence, String backupPath)  throws Exception {
        backupPath = createBackupDir(backupPath, prideMongoPeptideEvidence.getProjectAccession());
        String filename = prideMongoPeptideEvidence.getProjectAccession() + "_" + prideMongoPeptideEvidence.getAssayAccession() + "_" + PrideMongoPeptideEvidence.class.getName() + JSON_EXT;
        String backupLocation = backupPath + File.separator + filename;
        objectMapper.writeValue(new File(backupLocation), prideMongoPeptideEvidence);
    }

    private static String createBackupDir(String backupPath, String projectAccession) throws AccessDeniedException {
        if (!backupPath.endsWith(File.separator)) {
            backupPath = backupPath + File.separator;
        }
        backupPath = backupPath + projectAccession;
        File file = new File(backupPath);
        if(file.exists() && file.isDirectory()) {
            return backupPath;
        }
        boolean mkdirs = file.mkdirs();
        if (!mkdirs) {
            throw new AccessDeniedException("Failed to create Dir : " + backupPath);
        }
        return backupPath;
    }

    public static PrideMongoProteinEvidence getPrideMongoProteinEvidenceFromBackup(String projectAccession, String assayAccession, String backupPath) {
        PrideMongoProteinEvidence prideMongoProteinEvidence = null;
        File file = new File(backupPath + File.separator + projectAccession + File.separator + projectAccession +  "_" + assayAccession + "_" + PrideMongoProteinEvidence.class.getName() + JSON_EXT);
        try {
            prideMongoProteinEvidence = objectMapper.readValue(file, PrideMongoProteinEvidence.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return prideMongoProteinEvidence;
    }

    public static PrideMongoPeptideEvidence getPrideMongoPeptideEvidenceFromBackup(String projectAccession, String assayAccession, String backupPath) {
        PrideMongoPeptideEvidence prideMongoProteinEvidence = null;
        File file = new File(backupPath + File.separator + projectAccession + File.separator + projectAccession + "_" + assayAccession + "_" + PrideMongoPeptideEvidence.class.getName() + JSON_EXT);
        try {
            prideMongoProteinEvidence = objectMapper.readValue(file, PrideMongoPeptideEvidence.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return prideMongoProteinEvidence;
    }
}
