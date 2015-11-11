#! /usr/bin/env bash

sbt "proxy/run proxy -s $1 -c 7q4m:AES/ECB/PKCS5Padding:128"

