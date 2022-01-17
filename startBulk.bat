git pull
java -jar .\target\NetworkTester-1.0-SNAPSHOT-jar-with-dependencies.jar Client --data-ip=192.168.1.9 --ntp=192.168.2.9:9999 --trace=25 -d=%1 --no-gui 192.168.2.9 10000 BULK DOWN
