#!/bin/bash

#
# Dump out some stuff for a bigdata federation.
#
# usage: configFile

source `dirname $0`/bigdataenv

java ${JAVA_OPTS} \
    -cp ${CLASSPATH} \
    org.embergraph.service.jini.util.DumpFederation \
     ${BIGDATA_CONFIG} ${BIGDATA_CONFIG_OVERRIDES}
