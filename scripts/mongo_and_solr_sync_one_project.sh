#!/usr/bin/env bash

# Load environment (and make the bsub command available)
. /etc/profile.d/lsf.sh

#This job syncs one document(accession based) from oracle into mongodb

##### OPTIONS
# (required) the project accession
PROJECT_ACCESSION=""

##### VARIABLES
# the name to give to the LSF job (to be extended with additional info)
JOB_NAME="mongo_and_solr_sync_one_project"
# memory limit
MEMORY_LIMIT=6000
# memory overhead
MEMORY_OVERHEAD=1000
# java memory limit
MEMORY_LIMIT_JAVA=0
# LSF email notification
JOB_EMAIL="pride-report@ebi.ac.uk"
# Log file path
LOG_PATH="./log/${JOB_NAME}/"
# Log file name
LOG_FILE_NAME=""

##### FUNCTIONS
printUsage() {
    echo "Description: In the revised archive pipeline, this will import one project information to mongoDB"
    echo "$ ./scripts/mongo_and_solr_sync_one_project.sh"
    echo ""
    echo "Usage: ./mongo_and_solr_sync_one_project.sh -a|--accession [--skipfiles]"
    echo "     Example: ./mongo_and_solr_sync_one_project.sh -a PXD011181"
    echo "     (required) accession         : the project accession"
    echo "     (optional) skipfiles         :  if set will skip syncing files"
}

SKIP_FILES="false"

##### PARSE the provided parameters
while [ "$1" != "" ]; do
    case $1 in
      "-a" | "--accession")
        shift
        PROJECT_ACCESSION=$1
        ;;
      "--skipfiles")
        SKIP_FILES="true"
        ;;
    esac
    shift
done

##### CHECK the provided arguments
if [ -z ${PROJECT_ACCESSION} ]; then
         echo "Need to enter a project accession"
         printUsage
         exit 1
fi

##### Set variables
JOB_NAME="${JOB_NAME}-${PROJECT_ACCESSION}"
DATE=$(date +"%Y%m%d%H%M")
LOG_FILE_NAME="${JOB_NAME}-${DATE}.log"
MEMORY_LIMIT_JAVA=$((MEMORY_LIMIT-MEMORY_OVERHEAD))

##### Change directory to where the script locate
cd ${0%/*}

#### RUN it on the production queue #####
bsub -M ${MEMORY_LIMIT} \
     -R "rusage[mem=${MEMORY_LIMIT}]" \
     -q production-rh74 \
     -g /pride/analyze_assays \
     -u ${JOB_EMAIL} \
     -J ${JOB_NAME} \
     ./runPipelineInJava.sh ${LOG_PATH} ${LOG_FILE_NAME} ${MEMORY_LIMIT_JAVA}m -jar revised-archive-submission-pipeline.jar --spring.batch.job.names=syncProjectToMongoAndSolrJob -Dspring-boot.run.arguments= --accession=${PROJECT_ACCESSION} --skipfiles=${SKIP_FILES}
