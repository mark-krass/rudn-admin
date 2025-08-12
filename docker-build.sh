#!/bin/bash

IMAGE_NAME="rudn-admin-image"
TAG="latest"

docker build -t "$IMAGE_NAME:$TAG" .