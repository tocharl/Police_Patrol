#NOM : Makefile
#OBJECT : Client-Server comunication with authentification with username and password(chiper in the .txt)
#HOWTO : Comp; RunServer; RunClient; Clean;
#AUTORE : Mattia Garofalo 53587 - Thomas Charlier 49582


Introduction:
	@echo -DO make Compile TO COMPILE THE .JAVA FILE 
	@echo -DO make RunServer TO RUN THE JAVA CODE FOR THE SERVER
	@echo -DO make RunClient TO RUN THE JAVA CODE FOR THE CLIENT
	@echo -DO make Clean FOR TO DELETE ALL THE .classe and .txt FILES.

Compile:
	javac sec/*.java

RunServer:
	
	java sec/ServerMain

RunClient:
	java sec/ClientMain
	
Clean:
	rm -r sec/*.class
	rm -r *.txt
