#!/bin/bash

# Load .env variables (ignore comments and blank lines)
export $(grep -v '^#' local.env | xargs)

# Run the jar, pipe stdout and stderr to log file
nohup java -jar selfreflectionbot-1.0.0.jar >/dev/null 2>&1 &
