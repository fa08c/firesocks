#! /usr/bin/env bash

sbt "proxy/run proxy -s http://localhost:8080/worker -c 7q4m:AES/ECB/PKCS5Padding:128"
