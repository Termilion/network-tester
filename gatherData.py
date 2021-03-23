import pandas as pd
import pickle as pkl
import matplotlib.pyplot as plt
import matplotlib
import json
import re

inPath = "./out"

interface = "eno1" # "wlan0_ap"
ip = "10.0.0.1"
ssKeyword = "tcp" 


statFile = open("%s/test.log" % (inPath))
serverLog = open("%s/server.log" % (inPath))

logTimes = []
addresses = []
names = []
gps = []
delays = []

for line in serverLog.readlines():
    split = line.split(";")
    logTimes.append(split[0])
    addresses.append(split[1])
    names.append(split[2])
    gps.append(split[3])
    delays.append(split[4])

serverLogData = {"time": logTimes, "adress": addresses, "name": names, "gp": gps, "delay": delays}
df_serverLogs = pd.DataFrame(data=serverLogData)

with open("%s/serverLog.pkl" % inPath, 'wb') as handle:
    pkl.dump(df_serverLogs, handle)

tcLine = False

times = []
ss_rcvQ = []
ss_sndQ = []
tc_sentBytes = []
tc_sentPkts = []
tc_dropped = []
tc_overlim = []
tc_requeues = []
tc_backlog = []
tc_qlen = []

tcpmem_mem = []
tcpmem_rmem = []
tcpmem_wmem = []

sock_inuse = []
sock_mem = []
sock_orphan = []
sock_tw = []
sock_alloc = []

tool = ""
ss_regex = re.compile(r"[^-.:,\(\)\[\]\w]+")

for line in statFile.readlines():
    if line.startswith("TRACE: "):
        times.append(int(line.split("TRACE: ")[1]))
    elif line.startswith("TOOL: "):
        tool = line.split("TOOL: ")[1].strip()
    else:
        if tool == "ss":
            if ip in line and ssKeyword in line:
                split = re.sub(ss_regex, ";", line).split(";")
                ss_rcvQ.append(split[1])
                ss_sndQ.append(split[1])
                tool = ""
        elif tool == "tc":
            for obj in json.loads(line):
                if obj["dev"] == interface:
                    tc_sentBytes.append(obj["bytes"])
                    tc_sentPkts.append(obj["packets"])
                    tc_dropped.append(obj["drops"])
                    tc_overlim.append(obj["overlimits"])
                    tc_requeues.append(obj["requeues"])
                    tc_backlog.append(obj["backlog"])
                    tc_qlen.append(obj["qlen"])
        elif tool == "tcpmem":
            split = line.split(":")
            if "tcp_mem" in split[0]:
                tcpmem_mem.append(split[1].split(" "))
            elif "tcp_rmem" in split[0]:
                tcpmem_rmem.append(split[1].split(" "))
            elif "tcp_wmem" in split[0]:
                tcpmem_wmem.append(split[1].split(" "))
        elif tool == "sock":
            if "TCP:" in line:
                values = line.split(": ")[1]
                sock_inuse.append(values.split("inuse ")[1].split(" ")[0])
                sock_orphan.append(values.split("orphan ")[1].split(" ")[0])
                sock_alloc.append(values.split("alloc ")[1].split(" ")[0])
                sock_tw.append(values.split("tw ")[1].split(" ")[0])
                sock_mem.append(values.split("mem ")[1].split(" ")[0])



tracingData = {
    "time": times,
    #"ss_rcvQ": ss_rcvQ,
    #"ss_sndQ": ss_sndQ,
    "tc_sentBytes": tc_sentBytes,
    "tc_sentPkts": tc_sentPkts,
    "tc_dropped": tc_dropped,
    "tc_overlim": tc_overlim,
    "tc_requeues": tc_requeues,
    "tc_backlog": tc_backlog,
    "tc_qlen": tc_qlen,
    "sock_inuse": sock_inuse,
    "sock_alloc": sock_alloc,
    "sock_orphan": sock_orphan,
    "sock_tw": sock_tw,
    "sock_mem": sock_mem,
    "tcpmem_mem": tcpmem_mem,
    "tcpmem_rmem": tcpmem_rmem,
    "tcpmem_wmem": tcpmem_wmem
}

df_tracingData = pd.DataFrame(data=tracingData)

with open("%s/tracingData.pkl" % inPath, 'wb') as handle:
    pkl.dump(df_tracingData, handle)

font = {
    'family' : 'DejaVu Sans',
    'weight' : 'normal'
}

matplotlib.rc("font", **font)

SMALL_SIZE = 10
MEDIUM_SIZE = 12
BIGGER_SIZE = 14

plt.rc('font', size=SMALL_SIZE)          # controls default text sizes
plt.rc('axes', titlesize=SMALL_SIZE)     # fontsize of the axes title
plt.rc('axes', labelsize=MEDIUM_SIZE)    # fontsize of the x and y labels
plt.rc('xtick', labelsize=SMALL_SIZE)    # fontsize of the tick labels
plt.rc('ytick', labelsize=SMALL_SIZE)    # fontsize of the tick labels
plt.rc('legend', fontsize=SMALL_SIZE)    # legend fontsize
plt.rc('figure', titlesize=BIGGER_SIZE)  # fontsize of the figure title

plt.plot(df_tracingData["time"], df_tracingData["tc_sentPkts"])
plt.show()