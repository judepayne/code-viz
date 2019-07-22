#!/bin/bash

lein clean
lein uberjar
lein native-image
