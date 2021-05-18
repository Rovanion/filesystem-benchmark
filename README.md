# filesystem-benchmark

May or may not benchmark a file system. Things it might be
benchmarking instead includes:

* The kernel's ability to schedule threads.
* Java's file IO primitives in java.nio.

## Installation

Clone the source code from https://github.com/Rovanion/filesystem-benchmark, then compile a standalone jar-file using leiningen:

```
lein uberjar
```

You will now find a file named `target/uberjar/filesystem-benchmark-<VERSION>-standalone.jar` on your system ready for usage.

## Usage

You will need to add the following to `/etc/sudoers` through `visudo`:

```
<your username here>   ALL = NOPASSWD: /sbin/sysctl vm.drop_caches=3
```

Then run the uberjar with Java 8 or above:

```
java -jar target/uberjar/filesystem-benchmark-<VERSION>-standalone.jar --help
```

## Options

```
  -p, --path PATH                                                 Path to the file system being tested.
  -s, --file-size NUM_BYTES                                       Size of the files to be generated in bytes.
  -c, --concurrency NUM_FILES                                     The number of files to write concurrently.
  -b, --benchmarks (read-throughput|write-throughput|write-type)  Which of the available benchmarks to run. Separate by comma if multiple.
```

## Examples

```
java -jar target/uberjar/filesystem-benchmark-<VERSION>-standalone.jar --path .
```


## License

Copyright © 2019 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
