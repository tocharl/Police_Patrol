# Protected Server-Client Communication in java language.

| first name | last name |   
|:----------:|:---------:|   
| Garofalo   | Mattia    |   
| Charlier   | Thomas    |  

## Table of content 
**[Description](#description)**  
**[Installation](#installation)**   
**[Usage](#usage)**     

## Description
The goal of the project is to have an exchange of information, between a client and a server, secure to avoid all types of cyber-attacks.

In this project the client can to be a:

            -Cop;
            -Captain;
            -Admin;
            -Gouvernemnt;

Avery different client can to do differents actions if is login.

Login Cop can:

        -Disconnection;
        -Create patrol;
        -Set current position;
        -Set destination;
        -Set external member;
        -Join existing patrol;

Login Captain can:

        -Disconnection;
        -Join existing patrol;
        -Add a cop to your supervision;
        -Get your patrols informations;
        -Set destination of a patrol;
        -Set external member of a patrol;

Login Admin can:

        -Disconnection;
        -Get waiting list of registrations;
        -Validate registration of one or multiple users;

Login Government can:
            



## Installation

$git clone https://git.esi-bru.be/49582/project2_sec5_49582_53587.git     
$cd PolicePatrol_sec5_49582_53587     
$make   

## Usage

The Makefile in the directory explain how to create the environment for the execution of  the program.

The server contain already some informations.
You can use this clients:

	-Cop 			Login: nicola 		psw: thebestcop1
	-Captain 		Login: capamerica 	psw: thebiggest1
	-Admin 		        Login: lucadimin 	psw: yuculele25
	-Government 	        Login: jacktheking  	psw: ihave6pack


For the execution:  
- Compile the java files : make Compile  
- Run the server : make RunServer  
- Run the client : make RunClient

Multiple clients can be connecting with the server at the same time.   
Do make Clean to delete the .class an .txt files.

