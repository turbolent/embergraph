#!/bin/bash

# Script starts an httpd server which may be used to view historical
# or post-mortem performance counters and/or events.

# usage: counters.sh [-p port] [-events events.log] counterFile(s)

source `dirname $0`/embergraphenv

java ${JAVA_OPTS} \
	-cp ${CLASSPATH}\
    org.embergraph.counters.httpd.CounterSetHTTPDServer $*
