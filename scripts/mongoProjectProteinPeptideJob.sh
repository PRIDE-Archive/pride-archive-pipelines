#!/usr/bin/env bash

# Load environment (and make the bsub command available)
. /etc/profile.d/lsf.sh

#This job import assay information into mongodb

##### OPTIONS
# (required) the project accession
PROJECT_ACCESSION=""

##### VARIABLES
# the name to give to the LSF job (to be extended with additional info)
JOB_NAME="mongo_project_peptide_protein"
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

##### FUNCTIONS
printUsage() {
    echo "Description: In the revised archive pipeline, This job indexes peptides & proteins to Solr from MongoDB"
    echo "$ ./scripts/mongoProjectProteinPeptideJob.sh"
    echo ""
    echo "Usage: ./mongoProjectProteinPeptideJob.sh -a|--accession"
    echo "     Example: ./mongoProjectProteinPeptideJob.sh -a PXD011181"
    echo "     (optional) accession         : the project accession"
}

JOB_ARGS=""

##### PARSE the provided parameters
while [ "$1" != "" ]; do
    case $1 in
      "-a" | "--accession")
        shift
        PROJECT_ACCESSION=$1
        JOB_ARGS="${JOB_ARGS} --project=${PROJECT_ACCESSION}"
        ;;
    esac
    shift
done

JOB_NAME="${JOB_NAME}-${PROJECT_ACCESSION}"
DATE=$(date +"%Y%m%d%H%M")
LOG_FILE_NAME="${JOB_NAME}-${DATE}.log"
MEMORY_LIMIT_JAVA=$((MEMORY_LIMIT-MEMORY_OVERHEAD))

##### CHECK the provided arguments
if [ -z ${PROJECT_ACCESSION} ]; then
         echo "Running for all projects as no project accession has been provided"
fi

##### Change directory to where the script locate
cd ${0%/*}

#### RUN it on the production queue #####
bsub -M ${MEMORY_LIMIT} \
     -R "rusage[mem=${MEMORY_LIMIT}]" \
     -q production-rh74 \
     -g /pride/analyze_assays \
     -u ${JOB_EMAIL} \
     -J ${JOB_NAME} \
     ./runPipelineInJava.sh ${LOG_PATH} ${LOG_FILE_NAME} ${MEMORY_LIMIT_JAVA}m -jar revised-archive-submission-pipeline.jar --spring.batch.job.names=mongoProjectProteinPeptideJobBean ${JOB_ARGS}

