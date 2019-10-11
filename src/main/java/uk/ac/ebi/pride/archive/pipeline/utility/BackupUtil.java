package uk.ac.ebi.pride.archive.pipeline.utility;

import com.fasterxml.jackson.databind.ObjectMapper;
import uk.ac.ebi.pride.mongodb.molecules.model.peptide.PrideMongoPeptideEvidence;
import uk.ac.ebi.pride.mongodb.molecules.model.protein.PrideMongoProteinEvidence;

import java.io.File;
import java.io.IOException;

/**
 * @author Suresh Hewapathirana
 */
public class BackupUtil {

    public static void backupPrideMongoProteinEvidence(PrideMongoProteinEvidence prideMongoProteinEvidence){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String filename = prideMongoProteinEvidence.getProjectAccession() + "_" + prideMongoProteinEvidence.getAssayAccession() + "_" +  "prideMongoProteinEvidence.json";
            String backupLocation = "target/" + filename;
            objectMapper.writeValue(new File(backupLocation), prideMongoProteinEvidence);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void backupPrideMongoPeptideEvidence(PrideMongoPeptideEvidence prideMongoPeptideEvidence) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String filename = prideMongoPeptideEvidence.getProjectAccession() + "_" + prideMongoPeptideEvidence.getAssayAccession() + "_" +  "PrideMongoPeptideEvidence.json";
            String backupLocation = "target/" + filename;
            objectMapper.writeValue(new File(backupLocation), prideMongoPeptideEvidence);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
