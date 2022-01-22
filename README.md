# Mozart Project

## Project description

This project aims to implement a multi-player concert, using akka actors. It contains the code to run a Concert's Node. 
Multiple (max 4) remote nodes can run in parallel and communicate together. The adress of the remote nodes can be found and modified in the `application.conf` file.
The first node to join becomes the conductor. He waits 10s for other nodes to join, and if none arrive he shuts down.
Once a musician has joined, the concert starts: the conductor throws dices to choose a measure, thus playing the [Mozart Game](https://en.wikipedia.org/wiki/Musikalisches_Würfelspiel).
He then sends the measure to one of the available musicians, who plays the measure. 
If a musicians shuts down, the conductor stops sending it measures. If all musicians are down, the conductor waits 10s for a musician to arrive, and otherwise shuts down.
If the conductor shuts down, the other running nodes run an election to determine who will be the next conductor, and the concert goes on. 

## Running the project

To start a Node, start the sbt server, compile then run the code with:
```console
sbt
compile
run <num>
```
<num> being the Node number you want to run (from 0 to 3).
  
When starting multiple actors, wait a few seconds for the first Node to be initialized before running the second (but don't wait to long, otherwise the first Node will stop!). The third and fourth node can then be started at any moment.

## Actor's architecture

![Architecture of the actors](/read_me_img/architecture.png)

At startup, all actors instantiate a single instance of their respective children.

## Communication between actors

![Messages exchanged by actors](/read_me_img/communication.png)

- The Heart is used to communicate the status of the whole Node to other actors. This status is either Dead, LivePlayer or LiveConductor. 
When instantiated, the status is LivePlayer, and it can be switched to LiveConductor if the Node is elected as conductor.
- The Checker regularly asks the 4 hearts their status. It then informs the Node of which musicians are available (useful for the Conductor to send measures). 
If the Checker finds that none of the musicians are LiveConductor, it tells the Node to launch an election.
- The Node is the center of the application. It keeps track of the musicians which are up and running (excluding the conductor). It is in charge of running an election when needed, and when elected it warns its child actors. 
- The Conductor only starts when the Node is elected conductor. 
It roles dices, and asks both the Node which musicians are available, and the provider to retrieve the measure corresponding to the dices. 
Once it has these 2 answers, it chooses a musician randomly among those available, and sends its player the measure. It repeats these tasks periodically.
If no musicians are available, the conductor waits 10s before trying to find new musicians, and if there are still none, it calls for the Node to stop with a ConductorStoppedException.
- The Provider calls for the Database only once, at startup, to retrieve the list of measures (which cannot be stored directly inside the provider because the file length exceeds the max authorized size).
It maps the dices and the counter to a given measure.

## Conductor election

To elect a conductor, we use a simple protocol: the node to become conductor is the **live node (status LivePlayer) with the smallest index**.
In that way, if nodes 1 and 2 are running with the status LivePlayer (and the other nodes are Dead) both Nodes will begin an election with the status list: [Dead, LivePlayer, LivePlayer, Dead].
Node 1 will become conductor, by changing its HeartStatus and starting the Conductor actor. Node 2 realizes he is not conductor, and does nothing.
This election method works as long as all nodes share the same status list. Because of asynchronous messaging, this list can diverge between Nodes. 
A common issue is that the Checker marks a Node as Dead because the Heart takes too long to answer, even if this Node is in fact alive.
A solution to solve this issue is to make the Checker's timeout longer (currently 1.5s). However, making it too long will induce delays in noticing that a node has changed status.  
