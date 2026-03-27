#!/bin/bash
export DB_URL='jdbc:postgresql://172.31.27.231:5432/chatflow'
export DB_USER=chatflow
export DB_PASS=chatflow
export RABBIT_HOST=172.31.21.132
export RABBIT_USER=chatflow
export RABBIT_PASS=chatflow123
export BROADCAST_THREADS=32
exec java -Xmx700m -jar /home/ubuntu/server.jar
