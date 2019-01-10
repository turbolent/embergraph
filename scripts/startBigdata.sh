#!/bin/bash

BASE_DIR=`dirname $0`

PORT=9999
PROPERTIES_FILE="${BASE_DIR}"/../embergraph-war-html/src/main/webapp/WEB-INF/RWStore.properties
NAMESPACE="kb"


"$BASE_DIR"/progBigdata.sh com.bigdata.rdf.sail.webapp.NanoSparqlServer $PORT $NAMESPACE $PROPERTIES_FILE  $*

