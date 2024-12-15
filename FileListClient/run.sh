#!/bin/bash

# Check if at least two arguments are provided
if [ "$#" -lt 2 ]; then
    echo "Usage: ./run.sh <server_IP1:port1> <server_IP2:port2>"
    exit 1
fi

# Pass all arguments to the Java program
java -cp "bin:lib/*" client.dummyClient "$@"
