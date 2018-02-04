========================== -[README]- ===========================

	-Group 7-

	Jack MacDougall - 101004427
	Joseph Laycano - 100996474
	Luke Mitchell - 100940523
	Eric Scott - 101015358
	Karl Schnalzer - 100982672

======================== -[Deliverables]- =======================

UML Class Diagram
- UML.PDF

README file
- README.PDF

UCM read file transfer and write file transfer Diagrams
- UCM Diagrams.PDF

Client Class
- Client.java

Server Class
- Server.java

ErrorSimulator Class
- ErrorSimulator.java

ServerThread Class
- ServerThread.java


==================== -[Set Up Instructions]- ====================
Assuming all files are open in Eclipse-java, do the following to properly run the system:
1) In the Server class, select the "Run As" option and select the "Java application" option.
2) In the ErrorSimulator class, select the "Run As" option and select the "Java application" option.
3) In the Client class, select the "Run As" option and select the "Java application" option.
4) In the Console, follow the given user prompts to run the system.
	A) You will be asked to enter a file name or to type 'exit' to shutdown the Client.
		- (EX: {file name example}     test.txt    )
	B) Once a file name is entered, the Client will ask you if it is a read (r) or write (w) request.
		- (EX: {response example for read request}     r     )
	C) If the User has selected a Read request, the console will prompt the user to enter the filename to be read.
	D) If the User has selected a Write request, the console will prompt the user to enter the path of the file to be written.
	E) After a request has finished, the Client will prompt the user for another request.

======================= -[Responibilities]- =====================

 	-[Iteration 0 & 1]-

	[Jack MacDougall]
- Provided base classes (from Assignment 1)
- Client class edits
- Server class edits
- ErrorSimulator class edits
- ServerThread class edits
- Debugging

	[Joseph Laycano]
- Client class edits
- Server class edits
- ErrorSimulator class edits
- ServerThread class edits
- Implemented Steady-State file transfer
- Debugging

	[Luke Mitchell]
- Server class edits
- README file
- UML class diagram
- UCM read
- UCM write
- Debugging

	[Eric Scott]
- Client class edits
- Final commenting of code
- Debugging

	[Karl Schnalzer]
- Client class edits
