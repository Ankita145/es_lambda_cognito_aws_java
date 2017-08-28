#!/usr/bin/env bash

aws lambda create-function \
--function-name AuthLambda \
--profile default \
--region us-east-1 \
--zip-file fileb://../target/es-lambda-example-1.0-SNAPSHOT.jar \
--handler example.handler.AuthHandler::handleRequest \
--role arn:aws:iam::322183482473:role/alex-lambda-execution \
--runtime java8 \
--timeout 30 \
--memory-size 1024
