SYSC3303-Assignment 1-Jack MacDougall(101004427)-README

Class - EchoClient:
Client side of a simple echo server.
The client sends a request packet to the intermediate host:
	Read requests will have these first two bytes: 0 1
	Write requests will have these first two bytes: 0 2
	An invalid request will have a 1 byte in between the filename and the mode instead of a 0 byte
It then recieves a reply packet from the intermediate host.

Class - EchoIntermediateHost:
Intemediate Host of a simple echo server.
The intermediate host receives from a client a request packet and then sends it to the server.
It then receives from the server a reply packet back to the original client

Class - EchoServer:
Server side of a simple echo server.
The server receives from a client a request packet sent from the intermediate host.
The server then parses the request packet:
	If it's a read request, the server will create a reply packet with bytes: 0 3 0 1
	If it's a read request, the server will create a reply packet with bytes: 0 4 0 0
	If it's an invalid request, the server will throw an exception and quit
It then sends a reply packet back to the intermediate host.



Set-Up:
1. Right click on EchoServer and select Run As >> Java Application
2. Right click on EchoIntermediateHost and select Run As >> Java Application
3. Right click on EchoClient and select Run As >> Java Application