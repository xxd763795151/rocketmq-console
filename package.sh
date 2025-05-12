#!/bin/bash

mvn clean package -Dmaven.test.skip -Dcheckstyle.skip=true
