# Consensus-project

The project represents an implementation in Java and Protobuf of a consesus communication between processes.

We have 3 processes which each choose a number. The processes transmit their number trough messages on the Application, Network and Physical layer using sockets.
Google Protobuf was used for defining the messages on each layer.
