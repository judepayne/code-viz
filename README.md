# code-viz

Create visualisations of source code directory structures.

![Clojure src visualized!](clojure.svg)

This command line tool/ clojure library will crawl a specified source directory tree and the files within it, counting the lines in each files and categorizing the languages used. The visualization of this information is done with graphviz; statistics are summed up the (directory) tree.


## Usage

*Note* This tool requires graphviz to be installed on your computer.

#### As a commmand line tool

several variants are provided: a jar, a native Mac OS binary (compiled using Graal) and a native Windows exectuable (ditto).

##### jar

In the `target` directory, find the standalone jar.

    java jar code-viz-0.1.0-standalone.jar -h
    
will bring up the help.

##### Mac OS and Windows

In the `target` directory are sub dirs for `mac` and for `windows`. Within each their is a native executable that has been compiled using Graal from Oracle. The native executable avoids jvm startup time and hence runs alot faster.

Use as follows (e.g. the `mac` one)

    ./target/mac/code-viz -h


#### As a Clojure library

    (use 'code-viz.core)
    
The function to use is path->svg, which takes the following arguments:

| argument | description |
|----------|-------------|
| path | the path to the root of the (source) directory you wish to visualize. |
| filename | the visualization output file (svg format) |

In addition, there are two optional arguments:

| argument | default | description |
|----------|---------|-------------|
| graphviz-path | 'dot' | the path of the graphviz executable e.g. /opt/bin/dot |
| exclusions | [] | A vector of regexes which if any match a directory name, that directory is not explored furhter |


## License

Copyright © 2019 Jude Payne

Distributed under the MIT License
