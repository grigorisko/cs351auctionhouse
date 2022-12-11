### Project 5: Auction Distribution by Fermin Ramos, Vasileios Grigorios Kourakos and Loc Tung Su


### General Description
Three program Bank, Agent, AuctionHouse, that can interact with each other through
Socket connection and complete different tasks.

1. The Bank (Bank.jar) can be considered the middle ground where it only plays as a server that connect the Agents to the AuctionHouses.

2. The AuctionHouse (AuctionHouse.jar) is play as both a Server and a Client. It is a Client to the Bank, but it is also a Server to the Client. The AuctionHouse job is reading in the Agents' request and communicate it with the Bank and 
to complete different tasks.

3. The Agent (Agent.jar)  is the one that will mostly initiate all the action such as making bids, set bank balance, etc

### BIDDING Status
Four bidding status:
1. `ACCEPTED`
2. `REJECTED`
3. `OUTBID`
4. `WINNER`

`ACCEPTED` status is set when a bid is being requested, and it meets all the requirements.\
`REJECTED` status is set when a bid is being requested, and it does not meet all the requirements.\
`OUTBID` status is set when you are the current bid winner, and another agent makes a better the bid than you, your bid will be OUTBID-ED.\
`WINNER` status is set when after the 30 seconds interval has passed, and no other Agents contest your bid, you will win the bidding process.

### GUI 
_**No GUI**_

### How to run
The program can run on the same computer or through multiple computer through jar file.\
For one computer through IntelliJ
**Steps** 
1. Run the Bank.jar by left click on file -> Run jar. It will give you the host name. Copy it.
2. Run the AuctionHouse.jar by left click on file -> Run jar. It will ask you the host name which you will put the host
name provided when running the Bank.jar. You can run as much AuctionHouse instances as you want.Then input the wanted name, i.e ah1, ah2, ...
3. Run the Agent.jar by left click on file -> Run jar. It will ask you the host name which you will put the host
name provided when running the Bank.jar. You can run as much Agent instances as you want. Then input the wanted name, i.e user1, user2, ...
4. After running the Agent, you will be ask to input in the Bank Balance which will be your total money you can spend
on the bidding.
5. A menu will pop with different set of options.
![img.png](resources/img.png)
6. Using the index of the option to choose the option. 1, 2 or 3.
7. When viewing the available AuctionHouses, you can choose a specific AuctionHouse base on the name of it applying step 6.
![img_1.png](resources/img_1.png)
8. Choose a specific AuctionHouse, then apply step 6 to make bid with the following format bid "ItemIndex bidAmount". For example:
![img_2.png](resources/img_2.png)
In the bottom, "1" is the "ItemIndex" for the first item and "4" is the "bidAmount".
9. Make sure your bid to an item has to be larger than the CurrentBid + MinimumBid to be `ACCEPTED`.

For multiple computers through terminal
**Steps**
1. After create the three java files, Bank.jar, AuctionHouse.jar and Agent.jar, for the UNM CS machine, we need to copy and paste 
it to the `BIN` location of our JDK/ZULU file. This has to be applied for all computers that are in used to run this project.
2. In the `BIN` location, where we put our Jar files, open terminal in that location for each computer.
3. Assuming we are only using 3 computers, one computer will be in charged of the Bank.jar, one for AuctionHouse.jar, one for Agent.jar.
4. Run the Bank.jar by typing `./java -jar Bank.jar` in the terminal.
5. Run the AuctionHouse.jar by typing `./java -jar AuctionHouse.jar` in the terminal. It will ask you the host name which you will put the host
   name provided when running the Bank.jar. You can run as much AuctionHouse instances as you want.Then input the wanted name, i.e ah1, ah2, ...
6. Run the Agent.jar by typing `./java -jar Agent.jar` in the terminal. It will ask you the host name which you will put the host
   name provided when running the Bank.jar. You can run as much Agent instances as you want. Then input the wanted name, i.e user1, user2, ...
7. After running the Agent, you will be asked to input in the Bank Balance which will be your total money you can spend
   on the bidding.
8. A menu will pop with different set of options.
   ![img.png](resources/img.png)
9. Using the index of the option to choose the option. 1, 2 or 3.
10. When viewing the available AuctionHouses, you can choose a specific AuctionHouse base on the name of it applying step 6.
    ![img_1.png](resources/img_1.png)
11. Choose a specific AuctionHouse, then apply step 6 to make bid with the following format bid "ItemIndex bidAmount". For example:
    ![img_2.png](resources/img_2.png)
    In the bottom, "1" is the "ItemIndex" for the first item and "4" is the "bidAmount".
12. Make sure your bid to an item has to be larger than the CurrentBid + MinimumBid to be `ACCEPTED`.

### Team's participation
I am just happy to be here :D- **Loc** TODO: Write later


