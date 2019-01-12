#!/bin/bash

# Tests the statistics collector for the platform.
#
# usage: [interval [count]]
#
# See org.embergraph.counters.AbstractStatisticsCollector#main(String[])

source `dirname $0`/embergraphenv

java ${JAVA_OPTS} \
	-cp ${CLASSPATH} \
    org.embergraph.counters.AbstractStatisticsCollector \
    $*
