# Computations on the Edge in the Internet of Things

Originally written by Andreas Moregård Haubenwaller (andreas.moregard@gmail.com) and Konstantinos Vandikas (kvandikas@gmail.com)
Deployed here only for reference purposes.

## INFO:

In src/main/scala you have 4 directories:

deployment: This is the main directory, it contains the Deployer, 
the Scheduler and the Landmark actors as well as the main actorsystem.

IOPackage: This contains a trait that is used by the testdeployment

testdeployment: This is a testdeployment, it includes actorinfo and 
deviceinfo and sends this information to the landmark and deployer.
The actorinfo is the information that the "user" would send to the
deployer in order to get the actors deployed.

testsystem: This contains the actors that are used in the testdeployment

## HOW TO RUN THE TESTDEPLOYMENT:
In order to try the testdeployment, you need to start the five actor systems
that will be running the deployed actorsystem. This is done by starting five
different terminals/consoles.

In each console, start sbt. Now you should run the "DeploymentApp" with a 
specific portnumber. 

In console 1 type "run 6001", choose DeploymentApp,
In console 2, type "run 6002" choose DeploymentApp,
In console 3, type "run 6003" choose DeploymentApp,
In console 4, type "run 6004" choose DeploymentApp,
In console 5, type "run 6005" choose DeploymentApp


Now you have 5 running actorsystems that are ready to run actors. Start two
more consoles and run sbt. Lets call them console 6 and 7. One should run the Deployer,
,Scheduler and Landmark, and the other will send the testdeployment.

In console 6, type "run" and choose DeploymentApp

This will start up the main actorsystem and will start the Deployer, the
Scheduler and the Landmark.

In console 7, type "run" and choose TestCaseDeployment

This will start the testdeployment that will send information about the 
devices to the Landmark, and then information about which actors to run
to the Deployer.

## 💬 Citation

```bibtex
@article{HAUBENWALLER201529,
title = {Computations on the Edge in the Internet of Things},
journal = {Procedia Computer Science},
volume = {52},
pages = {29-34},
year = {2015},
note = {The 6th International Conference on Ambient Systems, Networks and Technologies (ANT-2015), the 5th International Conference on Sustainable Energy Information Technology (SEIT-2015)},
issn = {1877-0509},
doi = {https://doi.org/10.1016/j.procs.2015.05.011},
url = {https://www.sciencedirect.com/science/article/pii/S187705091500811X},
author = {Andreas Moregård Haubenwaller and Konstantinos Vandikas},
keywords = {iot, constraint-programming, actor model, edge computing},
}