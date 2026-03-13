#!/bin/bash
cd /home/kavia/workspace/code-generation/github-access-report-service-43886-43906/github_access_report_backend
./gradlew checkstyleMain
LINT_EXIT_CODE=$?
if [ $LINT_EXIT_CODE -ne 0 ]; then
   exit 1
fi

