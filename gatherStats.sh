#!/bin/bash

RATE=$1
DURATION=$2

FILE=$3
APPEND=$4

if [ -z ${APPEND+x} ];
then 
	echo "appending file $FILE"
else
	echo "cleaning output file $FILE"
	rm $FILE
fi

for (( i=0; i<=$DURATION; i+=$RATE)) 
do
	echo "[$FILE] traced values at: $i ms"
	echo "TRACE: $i" >> $FILE
	echo "TOOL: ss" >> $FILE
	ss -tmOHi >> $FILE
	echo "TOOL: tc" >> $FILE
	tc -s qdisc >> $FILE
	echo "TOOL: sock" >> $FILE
	cat /proc/net/sockstat >> $FILE
	echo "TOOL: tcpmem"
	grep . /proc/sys/net/ipv4/tcp*mem >> $FILE
	sleep $(expr $RATE / 1000)
done
