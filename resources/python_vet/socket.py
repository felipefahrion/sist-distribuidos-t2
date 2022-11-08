import socket
import struct
import json
import sys
import threading
import random
import time
import os


sem = threading.Semaphore(1)

multicast_addr = '224.0.0.1'
bind_addr = '0.0.0.0'
multicast_port = 3000

MY_ID = 0
MY_IP = "127.0.0.1"
MY_PORT = 5005
MY_CHANCE = 0.5
MY_EVENTS = 100
MY_MIN_DELAY = 0.2
MY_MAX_DELAY = 0.36

otherHosts = []

clocks = []

started = False
ThStart = ""
sendOK = False

def showData(event):
    global clocks
    clkVet = f"{MY_ID} ["
    x = 0
    for i in clocks:
        clkVet += str(i["clock"])
        if (x != len(clocks)-1):
            clkVet += ","
        else:
            clkVet += "]"
        x += 1
    
    if event["status"] == "L":
        clkVet += " L"
    elif event["status"] == "S":
        clkVet += f" S {event['destino']}"
    elif event["status"] == "R":
        clkVet += f" R {event['remetente']} {event['clock']}"

    print(clkVet)

def localIncrement():
    global clocks
    for i in clocks: 
        if i["id"] == MY_ID:
            sem.acquire()
            i["clock"] += 1
            sem.release()
    showData({"status": "L"})

def externalIncrement(_id):
    global otherHosts
    id = otherHosts[_id]["id"]
    ip = otherHosts[_id]["ip"]
    port = otherHosts[_id]["port"]
    msg = []
    global clocks
    for clk in clocks:
        if(clk["id"] == MY_ID):

            sem.acquire()
            clk["clock"] += 1
            sem.release()
        msg.append({"id": clk["id"], "clock": clk["clock"]})
    if(SendUDPMessage(msg, ip, port)):
        showData({"status": "S", "destino": id})
    else:
        os._exit(0)

def start():
    print("Start")
    global otherHosts
    i = 0
    while i < MY_EVENTS:
        i += 1
        if random.uniform(0.0, 1.0) < MY_CHANCE:
            id = random.randint(0, len(otherHosts)-1)
            externalIncrement(id)
        else:
            localIncrement()

        time.sleep(random.uniform(MY_MIN_DELAY, MY_MAX_DELAY))
    os._exit(0)

def actionMultiCastReceived(msg):
    if(msg["IPOrigem"] != MY_IP or msg["PortOrigem"] != MY_PORT):
        global started
        global ThStart
        if(msg["MSG"].lower() == "start" and started == False):
            started = True
            ThStart = threading.Thread(target=start, args=())
            ThStart.start()
        elif msg["MSG"].lower() == "close" or msg["MSG"].lower() == "exit":
            os._exit(0)

def recvMulticast():
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    membership = socket.inet_aton(multicast_addr) + socket.inet_aton(bind_addr)

    sock.setsockopt(socket.IPPROTO_IP, socket.IP_ADD_MEMBERSHIP, membership)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

    sock.bind((bind_addr, multicast_port))

    while True:
      dados, address = sock.recvfrom(4096)
      recebido=json.loads(dados.decode("utf-8"))
      actionMultiCastReceived(recebido)

def sendMulticast(text=""):
    msg = json.dumps({"IDOrigem": MY_ID, 
                        "IPOrigem": MY_IP, 
                        "PortOrigem": MY_PORT,
                        "MSG": text}).encode("utf-8")

    sock_send = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    ttl = struct.pack('b', 1)
    sock_send.setsockopt(socket.IPPROTO_IP, socket.IP_MULTICAST_TTL, ttl)
    sock_send.sendto(msg, (multicast_addr, multicast_port))
    sock_send.close()

def actionUDPReceived(msg):
    global clocks
    global sendOK
    if(msg["IPOrigem"] != MY_IP or msg["PortOrigem"] != MY_PORT):
        clockRecebido=-1
        if msg["MSG"] == "OK":
            sendOK = True
        else:
            incr = False
            sem.acquire()
            for i in clocks:
                if i["id"] == MY_ID :
                    i["clock"] += 1
                for ms in msg["MSG"]:
                    if ms["id"] == msg["IDOrigem"]:
                        clockRecebido = ms["clock"]
                
                    if i["id"] == ms["id"]:
                        if(i["clock"] < ms["clock"]):
                            i["clock"] = ms["clock"]
            
            sem.release()
            showData({"status": "R", "remetente": msg["IDOrigem"], "clock": clockRecebido})

def ReceiveUDPMessage():
    sock = socket.socket(socket.AF_INET, # Internet
                        socket.SOCK_DGRAM) # UDP

    sock.bind((MY_IP, MY_PORT))

    sockSend = socket.socket(socket.AF_INET, # Internet
                        socket.SOCK_DGRAM) # UDP
    while True:
        dt = sock.recvfrom(1024)
        data = json.loads(dt[0].decode("utf-8"))
        if data["MSG"] != "OK":
            msg = json.dumps({"IDOrigem": MY_ID, 
                            "IPOrigem": MY_IP, 
                            "PortOrigem": MY_PORT,
                            "MSG": "OK"}
                            ).encode("utf-8")
                        
            sockSend.sendto(msg, (data["IPOrigem"], data["PortOrigem"]))
        actionUDPReceived(data)

def SendUDPMessage(message, ip, port):

    sock = socket.socket(socket.AF_INET, # Internet
                        socket.SOCK_DGRAM) # UDP

    msg = json.dumps({"IDOrigem": MY_ID, 
                    "IPOrigem": MY_IP, 
                    "PortOrigem": MY_PORT,
                    "MSG": message}).encode("utf-8")
    
    sock.sendto(msg, (ip, port))

    global sendOK
    sendOK = False
    count = 0
    while sendOK == False:
        time.sleep(0.01)
        count += 1
        if(count == 1000):
            return False
    return True

def main(argv):
    print("main")
    file = argv[1]
    id = argv[2]

    if file == "-1" and id == "-1":
        sendMulticast("start")
        os._exit(0)

    data = []
    try:
        f = open(file)
        lines = f.readlines()
        data = lines[int(id) + 1].replace("\n","").split(" ")
        global MY_ID
        MY_ID = int(data[0])
        global MY_IP
        MY_IP = data[1]
        global MY_PORT
        MY_PORT = int(data[2])
        global MY_CHANCE
        MY_CHANCE = float(data[3])
        global MY_EVENTS
        MY_EVENTS = int(data[4])
        global MY_MIN_DELAY
        MY_MIN_DELAY = float(data[5])/1000
        global MY_MAX_DELAY
        MY_MAX_DELAY = float(data[6])/1000

        lines.pop(0)

        global otherHosts
        global clocks
        for line in lines:
            data = line.replace("\n","").split(" ")
            clocks.append({"id": int(data[0]), "clock": 0})
            if(MY_ID != int(data[0])):
                otherHosts.append({"id": int(data[0]), "ip": data[1], "port": int(data[2])})
            
    except IOError:
        print("Arquivo não encontrado!")
    finally:
        f.close()
        
    thUDP = threading.Thread(target=ReceiveUDPMessage, args=())
    thMult = threading.Thread(target=recvMulticast, args=())
    
    thUDP.start()
    thMult.start()

    thUDP.join()
    thMult.join()
    ThStart.join()

if __name__ == '__main__':
    if(len(sys.argv)!=3):
        print("Necessita de dois argumentos <configFile> <id>")
    else:
        main(sys.argv)
    #main(["","Config.cfg",0])