#! /usr/bin/env bash

sbt -Dfiresocks.cmdline="server -c 7q4m:AES/ECB/PKCS5Padding:128" "project web" run
