#!/bin/bash

#
# Dump out some stuff for a embergraph federation.
#
# usage: configFile

source `dirname $0`/bigdataenv

java ${JAVA_OPTS} \
    -cp ${CLASSPATH} \
    org.embergraph.zookeeper.DumpZookeeper \
     ${BIGDATA_CONFIG} ${BIGDATA_CONFIG_OVERRIDES}
