import pandas as pd;

statFile = open()
serverLog = open()

times = []
addresses = []
names = []
gps = []
delays = []

for line in serverLog.readlines():
    split = line.split(";")
    times.append(split[0])
    addresses.append(split[1])
    names.append(split[2])
    gps.append(split[3])
    delays.append(split[4])

serverLogData = {"time": times, "adress": addresses, "name": names, "gp": gps, "delay": delays}
df_serverLogs = pd.DataFrame(data=serverLogData)

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

for line in statFile.readlines():
    if line.startswith("TRACE: "):
        times.append(int(line.split("TRACE: ")[1]))
    elif line.startswith("TOOL: "):
        tool = line.split("TOOL: ")[1]
    else:
        if tool == "ss":
            if "java" in line:
                split = line.split("        ")
                ss_rcvQ.append(split[1])
                ss_sndQ.append(split[1])
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
    "times": times,
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
