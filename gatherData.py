import pandas as pd
import pickle as pkl
import matplotlib.pyplot as plt
import matplotlib
import re

inPath = "./out"

statFile = open("%s/statTrace.txt" % (inPath))
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
tc_backlogBytes = []
tc_backlogPkts = []
tc_backlogRequeues = []

tool = ""
ss_regex = re.compile(r"[^-.:,\(\)\[\]\w]+")

for line in statFile.readlines():
    if line.startswith("TRACE: "):
        times.append(int(line.split("TRACE: ")[1]))
    elif line.startswith("TOOL: "):
        tool = line.split("TOOL: ")[1].strip()
    else:
        if tool == "ss":
            if "CLOSE-WAIT" in line:
                split = re.sub(ss_regex, ";", line).split(";")
                ss_rcvQ.append(split[1])
                ss_sndQ.append(split[1])
                tool = ""
        elif tool == "tc":
            if "dev wlp2s0" in line:
                tcLine = True
            elif tcLine and "Sent" in line:
                sent = line.split("Sent ")[1];
                tc_sentBytes.append(int(sent.split(" bytes")[0]))
                tc_sentPkts.append(int(sent.split(" pkt")[0].split("bytes ")[1]))
                tc_dropped.append(int(sent.split("dropped ")[1].split(",")[0]))
                tc_overlim.append(int(sent.split("overlimits ")[1].split(" ")[0]))
                tc_requeues.append(int(sent.split("requeues ")[1].split(")")[0]))
            elif tcLine and "backlog" in line:
                backlog = line.split("backlog ")[1].split("requeues")[0]
                tc_backlogBytes.append(int(backlog.split("b")[0]))
                tc_backlogPkts.append(int(backlog.split("p")[0].split("b ")[1]))
                tc_backlogRequeues.append(int(line.split("requeues ")[1]))
                tcLine = False

tracingData = {
    "time": times,
    "ss_rcvQ": ss_rcvQ,
    "ss_sndQ": ss_sndQ,
    "tc_sentBytes": tc_sentBytes,
    "tc_sentPkts": tc_sentPkts,
    "tc_dropped": tc_dropped,
    "tc_overlim": tc_overlim,
    "tc_requeues": tc_requeues,
    "tc_backlogBytes": tc_backlogBytes,
    "tc_backlogPkts": tc_backlogPkts,
    "tc_backlogRequeues": tc_backlogRequeues
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

plt.plot(df_tracingData["time"], df_tracingData["ss_rcvQ"])
plt.show()