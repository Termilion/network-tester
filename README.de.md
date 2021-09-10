# Netzwerk Tester

Java CLI Programm zum Testen einer Netzwerkverbindung.

Dieses Programm wird in einem LAN, bestehend aus mehreren Knoten, verwendet und emuliert verschiedene Arten von Netzwerk
Traffic, wie IoT- & FTP-artigen Traffic. Eine Instanz kann eine der Rollen Server oder Client annehmen. Der Server
agiert neben der Rolle als zentraler Endpunkt für die Datenströme auch als Verwaltungs Knoten, welcher die Client Knoten
orchestriert. Alle restlichen Knoten werden als Client gestartet und verbinden sich zum Server. Jeder Client legt dabei
fest, ob er als Datenquelle (source) oder Datensenke agiert. Source-Knoten verschicken in einem bestimmten
Traffic-Pattern Daten. Sink-Knoten sammeln Metriken zu ihrem zugehörigen Datenfluss und loggen diese aggregiert (
Durchschnitt über einen Zeitintervall) in eine CSV Datei. Anchließend werden die Daten aller Knoten zu einem
Master-Knoten (Server) übertragen und in eine final CSV Datei gemerged.

# Metriken

Sämtliche Metriken werden über einen Zeitintervall erhoben (standardmäßig 50ms) und anschließend über diesen Intervall
gemittelt. An den Sinks erhobene Metriken sind:

1. Goodput (Mbps)
2. Delay der Pakete (ms)

# Ablauf

1. Zeitsynchronisierung
   - Clients verbinden sich mit dem Server
   - Server hostet NTP Server
   - Clients syncen einen internen Zeitgeber
2. Initialer Handshake
   - Clients senden Traffic-Patern und Flussrichtung an den Server
   - Server und Clients erstellen ihre respektiven Datenquellen und Datensenken
   - Server sendet Zeitstempel für den Simulationsbeginn an alle Knoten
3. Datenfluss
   - Server und alle Clients öffnen ihre Sinks und ihre Sources
   - Daten fließen
   - Source Knoten lassen Daten in bestimmen Pattern fließen
   - Sink Knoten loggen Metriken
4. Post Handshake
   - Alle Sink-Clients senden ihre CSV Datei an den Server
   - Server sendet an alle Clients ob sie sich anschließend für einen weiteren Run erneut verbinden sollen

# Ausgabe

Die final gemergten CSV Dateien liegen im `./out/` Ordner mit den Namen `hardware-%d-goodput.csv` (und enthalten anders
als ihr Name implizieren würde, auch den Delay).

# Logging

Das Programm loggt Statusinformationen auf `stdout` und gibt für jede Source und Sink eine realtime Grafik über die
aktuellen Übertragungsraten und Delays aus.

# Befehle

CLI besitzt --help Attribut

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
