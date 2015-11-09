#! /usr/bin/env bash

sbt "proxy/run server -b 0.0.0.0:8080 -c 7q4m:AES/ECB/PKCS5Padding:128"
