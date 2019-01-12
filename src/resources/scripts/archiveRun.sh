#!/bin/bash

# Archives the various files which we need for post-mortem analysis of
# an experimental run. The files are copied into the specified target
# directory and then a tarball is made of that directory.
#
# Note: You need to run this on the host that is running the load balancer.
# It will send a HUP signal to the LBS in order to force the production of
# a snapshot of the performance counters.  It will also need access to the
# service directory for the LBS so that it can copy the performance counters
# into the target directory.
#
# Note: You also need to collect the run from the console.

# usage: targetDir

if [ -z "$1" ]; then
	echo $"usage: $0 <targetDir>"
	exit 1
fi

BINDIR=`dirname $0`
source $BINDIR/bigdataenv

targetDir=$1

echo "COLLECT RUN FROM CONSOLE!"

mkdir -p $targetDir
mkdir -p $targetDir/counters
mkdir -p $targetDir/indexDumps

# Broadcast a HUP request to the load balancer in the federation so 
# that it will write a snapshot of its performance counters.
waitDur=60
echo "Sending HUP to the LoadBalancer: $pid"
$BINDIR/broadcast_sighup local loadBalancer
echo "Waiting $waitDur seconds for the performance counter dump."
sleep $waitDur

# Copy the configuration file and the various log files.
cp -v $BIGDATA_CONFIG \
   $eventLog* \
   $ruleLog* \
   $errorLog* \
   $detailLog* \
   $targetDir

# the journal containing the events (and eventually the counters).
cp -v $LAS/LoadBalancerServer/logicalService*/*/events.jnl $targetDir

# text files containing the logged performance counters.
cp -v $LAS/LoadBalancerServer/logicalService*/*/counters* $targetDir/counters

# Copy the index dumps if you are running the lubm test harness.
if [ -d "$NAS/lubm" ]; then
	cp -vr $NAS/*indexDumps* $targetDir/indexDumps
fi

# Extract performance counters for analysis.
#
# Note: This is easier to do on a server class machine, but it will nail the
# CPU for a few minutes while it processes the counter set XML files so you
# need to be sure that nothing will be adversely effected by that.
# 
# Note: This will only execute if it is run from the directory containing the
# embergraph source (it looks for the queries to run in a known location but also
# has a dependency on the build.properties and build.xml files for ant).
#
# Note: This creates a 2nd archive with just the extracted performance counters,
# the configuration file, the rule execution log, and the error log.
#
if [ -d "src/resources/analysis/queries" ]; then
	ant \
		"-Danalysis.counters.dir=$targetDir/counters"\
		"-Danalysis.queries=src/resources/analysis/queries"\
		"-Danalysis.out.dir=$targetDir/output"\
		analysis
	tar -cvz -C "$targetDir/.." -f $targetDir-output.tgz $targetDir/output \
	$BIGDATA_CONFIG \
   	$ruleLog* \
   	$errorLog* 
fi

tar -cvz -C "$targetDir/.." -f $targetDir.tgz $targetDir

echo "ready: $targetDir.tgz"
