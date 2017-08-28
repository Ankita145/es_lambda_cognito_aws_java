#!/usr/bin/env bash

# start jmeter GUI

source ~/scripts/proxy.sh

jmeter -t api.jmx -H $phost -P $pport -u $puser  -a $ppassword -N localhost -JCookieManager.save.cookies=true

