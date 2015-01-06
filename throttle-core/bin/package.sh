#!/bin/sh

# vars
HDFS=""
PROFILE="submit"

# parse the options
OPTS=`getopt -a -l hdfs::,profile: -- "$0" "$@"`
if [ $? != 0 ]
then
  	exit 1
fi

# replace and eval the args
eval set -- "$OPTS"
while true; do
	case "$1" in 
    	--hdfs) if [ -n "$2" ]; then HDFS="$2"; else HDFS="/snl/services/throttle/lib"; fi; shift; shift;;
    	--profile) PROFILE="$2"; shift; shift;;
    	--) shift; break;;
  	esac
done

# package using the submit profile
echo "Packaging with $PROFILE profile ..."
mvn clean package -P$PROFILE

# copy to hdfs if appropriate
if [ -n "$HDFS" ]
then
	echo "Copying file to hdfs: $HDFS"
	hdfs dfs -put -f lib/throttle-core-0.0.1.jar $HDFS 
fi


