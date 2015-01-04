#!/bin/sh

# inputs
HDFS=""

# parse the options
OPTS=`getopt -a -l hdfs:: -- "$0" "$@"`
if [ $? != 0 ]
then
  	exit 1
fi

# replace and eval the args
eval set -- "$OPTS"
while true; do
	case "$1" in 
    	--hdfs) if [ -n "$2" ]; then HDFS="$2"; else HDFS="/snl/services/throttle/lib"; fi; shift; shift;;
    	--) shift; break;;
  	esac
done

# package using the submit profile
mvn clean package -Psubmit

# copy to hdfs if appropriate
if [ -n "$HDFS" ]
then
	echo "Copying file to hdfs: $HDFS"
	hdfs dfs -put -f lib/throttle-core-0.0.1.jar $HDFS 
fi


