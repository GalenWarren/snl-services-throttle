#!/bin/sh

# default properties
PROPS=''

# parse the options
OPTS=`getopt -a -l keyCount:,period:,maxHits: -- "$0" "$@"`
if [ $? != 0 ]
then
  exit 1
fi

# replace the args and build up properties
eval set -- "$OPTS"
while true; do
  case "$1" in 
    --keyCount) PROPS="$PROPS -Dsnl.services.throttle.util.stress.keyCount=$2"; shift; shift;;
    --period) PROPS="$PROPS -Dsnl.services.throttle.util.stress.period=$2"; shift; shift;;
    --maxHits) PROPS="$PROPS -Dsnl.services.throttle.util.stress.maxHits=$2"; shift; shift;;
    --) shift; break;;
  esac
done

# execute the command
java $PROPS -jar lib/throttle-util-0.0.1.jar com.snl.services.throttle.util.Stress | grep Generated
