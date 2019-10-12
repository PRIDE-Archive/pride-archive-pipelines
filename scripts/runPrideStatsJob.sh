#!/usr/bin/env bash

# Load environment (and make the bsub command available)
. /etc/profile.d/lsf.sh

##### VARIABLES
# the name to give to the LSF job (to be extended with additional info)
JOB_NAME="pride-stats-data"
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

##### Set variables
DATE=$(date +"%Y%m%d%H%M")
LOG_FILE="${JOB_NAME}-${DATE}.log"
MEMORY_LIMIT_JAVA=$((MEMORY_LIMIT-MEMORY_OVERHEAD))

##### Change directory to where the script locate
cd ${0%/*}

#### RUN it on the production queue #####
bsub -M ${MEMORY_LIMIT} \
     -R \"rusage[mem=${MEMORY_LIMIT}]\" \
     -q research-rh74 \
     -g /pride/analyze_assays \
     -u ${JOB_EMAIL} \
     -J ${JOB_NAME} \
     ./runPipelineInJava.sh \
     ${LOG_PATH} \
     ${LOG_FILE_NAME} \
     ${MEMORY_LIMIT_JAVA}m \
     -jar revised-archive-submission-pipeline.jar \
     --spring.batch.job.names=computeSubmissionStatsJob