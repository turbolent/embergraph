#!/bin/bash

# Lists the discovered services.
#
# Note: You can override the repeatCount by adding the following to the
# command line.
#
# org.embergraph.service.jini.util.ListServices.repeatCount=0

source `dirname $0`/embergraphenv

java ${JAVA_OPTS} \
	-cp ${CLASSPATH} \
    org.embergraph.service.jini.util.ListServices \
    ${BIGDATA_CONFIG} ${BIGDATA_CONFIG_OVERRIDES} $*
