#!/usr/bin/env bash

# Load environment (and make the bsub command available)
. /etc/profile.d/lsf.sh

#This job syncs all documents from oracle into mongodb

##### VARIABLES
# the name to give to the LSF job (to be extended with additional info)
JOB_NAME="spring_batch_sdrf_mongo_and_solr_sync"
# memory limit
MEMORY_LIMIT=6000
# memory overhead
MEMORY_OVERHEAD=1000
# LSF email notification
JOB_EMAIL="pride-report@ebi.ac.uk"
# Log file path
LOG_PATH="./log/${JOB_NAME}/"
# Log file name
LOG_FILE_NAME=""

printUsage() {
    echo "Description: In the revised archive pipeline, this will import sdrf files from oracle to mongo and solr"
    echo "$ ./scripts/spring_batch_sdrf_mongo_and_solr_sync.sh"
    echo ""
    echo "Usage: ./spring_batch_sdrf_mongo_and_solr_sync.sh"
    echo "     Example: ./spring_batch_sdrf_mongo_and_solr_sync.sh"
}

##### Set variables
DATE=$(date +"%Y%m%d%H%M")
LOG_FILE_NAME="${JOB_NAME}-${DATE}.log"
MEMORY_LIMIT_JAVA=$((MEMORY_LIMIT-MEMORY_OVERHEAD))

##### Change directory to where the script locate
cd ${0%/*}


SKIP_FILES="false"

##### PARSE the provided parameters
while [ "$1" != "" ]; do
    case $1 in
      "--skipfiles")
        SKIP_FILES="true"
        ;;
    esac
    shift
done

while true; do
	read -p $'Are you sure you want to sync \e[1;31mALL\e[0m records(y/n)?' answer
    case $answer in
        [Yy]* ) bsub -M ${MEMORY_LIMIT} \
                     -R \"rusage[mem=${MEMORY_LIMIT}]\" \
                     -q production-rh74 \
                     -g /pride/analyze_assays \
                     -u ${JOB_EMAIL} \
                     -J ${JOB_NAME} \
                     ./runPipelineInJava.sh ${LOG_PATH} ${LOG_FILE_NAME} ${MEMORY_LIMIT_JAVA}m -jar revised-archive-submission-pipeline.jar --spring.datasource.maxPoolSize=10 --spring.batch.job.names=syncSdrfOracleToMongoFilesJob;
				break;;
        [Nn]* ) exit;;
        * ) echo "Please answer yes(y) or no(n).";;
    esac
done