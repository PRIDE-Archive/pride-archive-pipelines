package uk.ac.ebi.pride.archive.pipeline.utility;

/**
 * This class contains a set of constants that are needed to process the data in the submission pipeline.
 *
 * @author ypriverol
 */
public class SubmissionPipelineConstants {

   public enum SubmissionsType{
        ALL, PUBLIC, PRIVATE
   }

   public enum PrideArchiveJobNames{
       PRIDE_ARCHIVE_SOLR_CLOUD_INIT("PrideArchiveSolrCloudInit", "This command will create a new Collection of PRIDE Archive in SolrCloud Production.");

       String name;
       String message;

       PrideArchiveJobNames(String name, String message) {
           this.name = name;
           this.message = message;
       }

       public String getName() {
           return name;
       }

       public String getMessage() {
           return message;
       }

   }

    public enum PrideArchiveStepNames{
        PRIDE_ARCHIVE_SOLR_CLOUD_DELETE_COLLECTION("PrideArchiveSolrCloudDeleteCollectionStep", "This Step will delete the collection PRIDE Archive in SolrCloud Production."),
        PRIDE_ARCHIVE_SOLR_CLOUD_CREATE_COLLECTION("PrideArchiveSolrCloudCreateCollectionStep", "This Step will create the collection PRIDE Archive in SolrCloud Production."),
        PRIDE_ARCHIVE_SOLR_CLOUD_REFINE_COLLECTION("PrideArchiveSolrCloudRefineCollectionStep", "This Step will refine the collection PRIDE Archive in SolrCloud Production.");



        String name;
        String message;

        PrideArchiveStepNames(String name, String message) {
            this.name = name;
            this.message = message;
        }

        public String getName() {
            return name;
        }

        public String getMessage() {
            return message;
        }

    }
}