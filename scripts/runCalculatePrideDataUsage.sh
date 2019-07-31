#!/bin/sh

# Load environment (and make the bsub command available)
. /etc/profile.d/lsf.sh

# job details
JOB_NAME="archive-data-usage"
DATE=$(date +"%Y%m%d%H%M")
EMAIL="pride-report@ebi.ac.uk"
LOG_PATH="/nfs/pride/work/archive/revised-archive-submission-scripts/log/archive-data-usage-${DATE}.log"
JAR_PATH="/nfs/pride/work/archive/revised-archive-submission-pipeline/revised-archive-submission-pipeline.jar"
JAVA_DIR="/nfs/pride/work/java/jdk1.8.0_144/bin/"

# submit the job to LSF
bsub -M 3000 -R "rusage[mem=3000]" -q production-rh7 -u ${EMAIL} -J ${JOB_NAME} "${JAVA_DIR}java -Xmx2500m -jar ${JAR_PATH} --spring.batch.job.names=calculatePrideArchiveDataUsage > ${LOG_PATH} 2>&1"
