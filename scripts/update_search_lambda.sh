#!/usr/bin/env bash

aws lambda update-function-code \
--function-name EsLambda \
--profile default \
--region us-east-1 \
--zip-file fileb://../target/es-lambda-example-1.0-SNAPSHOT.jar
