#!/bin/sh

# default inputs
MASTER="yarn-client"
DEPLOY_MODE="cluster"


# parse the options
OPTS=`getopt -a -l master:,mode: -- "$0" "$@"`
if [ $? != 0 ]
then
  exit 1
fi

# replace the args
eval set -- "$OPTS"

while true; do
  case "$1" in 
    --master) MASTER=$2; shift; shift;;
    --) shift; break;;
  esac
done

# execute the command
$SPARK_HOME/bin/spark-submit --class akka.Main --master $MASTER --deploy-mode $DEPLOY_MODE [jar] com.snl.services.throttle.Main 
