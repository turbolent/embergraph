#!/bin/bash

# Setup the environment.
source src/resources/HAJournal/HAJournal.env

java\
 ${JAVAOPTS}\
 -cp ${CLASSPATH}\
 -Djava.security.policy=${POLICY_FILE}\
 -Dlog4j.configuration=${LOG4J_CONFIG}\
 -Djava.util.logging.config.file=${LOGGING_CONFIG}\
 org.embergraph.counters.AbstractStatisticsCollector 1 10
