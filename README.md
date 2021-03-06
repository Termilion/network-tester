# Network Tester

Java CLI program for testing a network connection.

This program is used in a LAN consisting of multiple nodes and emulates different types of network Traffic, such as IoT & FTP type traffic. An instance can adopt one of the roles: server or client. The server in addition to acting as a central endpoint for the data streams, also acts as a management node, which orchestrates the client nodes. All remaining nodes are started as clients and connect to the server. Each client determines whether it acts as a data source or a data sink. Source nodes send data in a specific traffic pattern. Sink nodes collect metrics about their associated data flow and log them aggregated (average over a time interval) to a CSV file. Subsequently, the data of all nodes are transferred to a master node (server) and merged into a final CSV file.

# Metrics

All metrics are collected over a time interval (by default 50ms) and then averaged over this interval. averaged over this interval. Metrics collected at the sinks are:

1. goodput (Mbps)
2. delay of packets (ms)

# Procedure

1. time synchronization
   - Clients connect to the server
   - Server hosts NTP server
   - Clients sync an internal timer
2. initial handshake
   - Clients send traffic pattern and flow direction to server
   - Server and clients create their respective data sources and data sinks
   - Server sends simulation start timestamp to all nodes
3. data flow
   - Server and all clients open their sinks and their sources
   - Data will flow
   - Source nodes flow data in specified pattern
   - Sink nodes log metrics
4. post handshake
   - All sink clients send their CSV file to server
   - Server sends to all clients if they should reconnect for another run afterwards

# Output

The final CSV files are located in the `./out/` folder named `hardware-%d-goodput.csv` (and, contrary to what their name would imply, contain than their name would imply, they also contain the delay).

# Logging

The program logs status information to `stdout` and outputs a realtime graph of current transfer rates and delays for each source and sink.

# Commands

CLI has --help attribute

``` bash
$ java -jar NetworkTester-1.0-SNAPSHOT-jar-with-dependencies.jar 
Missing required subcommand
Usage: <main class> [COMMAND]
Commands:
  Server  Starts an instruction server, which clients can connect to.
  Client  Starts a client which connects to the instruction server.
```

```bash
$ java -jar NetworkTester-1.0-SNAPSHOT-jar-with-dependencies.jar Server
Missing required parameter: '<expectedNumberOfClients>'
Usage: <main class> Server [--no-gui] [--runs=<runs>] [-t=<simDuration>]
                           [--trace=<traceIntervalMs>] ([--ntp=<ntpAddress>] |
                           [--ntpServerPort=<ntpServerPort>] |
                           [--distributedTime]) <port> <expectedNumberOfClients>
Starts an instruction server, which clients can connect to.
      <port>                 port to start server on
      <expectedNumberOfClients>
                             The expected number of clients. Once all clients
                               are connected the simulation will start
      --distributedTime      Sync time in a local distributed manner
      --no-gui               do not plot metrics in a gui window
      --ntp=<ntpAddress>     Address of a ntp server to sync time
      --ntpServerPort=<ntpServerPort>
                             Start a ntp server on this machine with the given
                               number as ntp port. The local time will be used,
                               to sync incoming client requests. The server
                               additionally uses a ntpClient against its own
                               ntpServer
      --runs=<runs>          Number of repetitions that are performed
  -t, --time=<simDuration>   Simulation duration in seconds.
      --trace=<traceIntervalMs>
                             Trace interval in ms.
```

```bash
$ java -jar NetworkTester-1.0-SNAPSHOT-jar-with-dependencies.jar Client
Missing required parameters: '<address>', '<port>', '<mode>', '<direction>'
Usage: <main class> Client [--no-gui]
                           [--ctrl-interface=<controlNetworkInterface>]
                           [-d=<startDelay>]
                           [--data-interface=<dataNetworkInterface>]
                           [--id=<id>] [-r=<resetTime>] [-rb=<rcvBuf>]
                           [-sb=<sndBuf>] [--trace=<traceIntervalMs>]
                           ([--ntp=<ntpAddress>] | [--distributedTime])
                           <address> <port> <mode> <direction>
Starts a client which connects to the instruction server.
      <address>              ipv4 address to connect to
      <port>                 port to connect to
      <mode>                 the application mode: IOT, BULK
      <direction>            the direction in which the data will flow
                               (respective from the Client point of view): UP,
                               DOWN
      --ctrl-interface=<controlNetworkInterface>
                             specifies a network interface (eth0, wifi0, ...)
                               which will be explicitly used for the transfer
                               of control messages, like time sync requests or
                               application setup (initial and post handshake
                               between clients and server).
  -d, --delay=<startDelay>   additional time to wait before transmission
      --data-interface=<dataNetworkInterface>
                             specifies a network interface (eth0, wifi0, ...)
                               which will be explicitly used for the transfer
                               of the "simulation" data.
      --distributedTime      Sync time in a local distributed manner
      --id=<id>              The id of this node. If not set, id will be
                               sequentially chosen by the server.
      --no-gui               do not plot metrics in a gui window
      --ntp=<ntpAddress>     Address of a ntp server to sync time
  -r, --resetTime=<resetTime>
                             time after the app gets forcefully reset in
                               milliseconds
      -rb, --rcvBuf=<rcvBuf> size of the tcp receive buffer in bytes
      -sb, --sndBuf=<sndBuf> size of the tcp send buffer in bytes
      --trace=<traceIntervalMs>
                             Trace interval in ms.
```

# Executable

`./target/NetworkTester-1.0-SNAPSHOT-jar-with-dependencies.jar`
