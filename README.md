# CloudNet-ManagementSocket
This resource allows you to manage CloudNet using a client-server architecture
![Sample usage of a CloudNet Console](docs%2Fimg%2Fsample-usage.png)

## Features
- Accidental kill prevention:
  - `stop`, `exit` and `shutdown` do only exit the console, which does not stop the CloudNet process
  - To stop the node, you need to go to the screen
  - `CTRL-D` or `CTRL-C` does only exit the console, which you can just recreate
- Tight integration into CloudNet ensures high compatability, even when using custom modules
- Full TabCompletion Support
- Ability to start multiple sessions


## Installation
The installation is done by moving the file [cloudnet-managementsocket.jar](https://github.com/EinDev/CloudNet-ManagementSocke/releases/latest/download/cloudnet-managementsocket.jar) to the `modules` folder of CloudNet.
It generated a config file, where you can specify where it should place the socket file.
Then you can start your node as usual. You don't need to use the screen anymore for anything except if you want to stop the node.

Currently only Linux is supported, if there is need for Windows Support, just create an issue.

## Configuration
Currently there is only a single configuration option where you can specify the socket path.
It is relative to the root folder of CloudNet. Keep in mind: Every user, who has access to this socket, has privileged access to CloudNet.
As of right now there is no authentication or encryption inside the socket. Please ensure security by properly securing the socket file.
````json
{
  "socketPath": "./cloudnet.socket"
}
````

## Usage
To use the module, you need the second file, [cloudnet-client.jar](https://github.com/EinDev/CloudNet-ManagementSocket/releases/latest/download/cloudnet-client.jar).
If you are using the default socket path, you can just run the JAR inside the root directory of CloudNet.
If not, you can use `java -jar cloudnet-client.jar -s /path/to/your/cloudnet.socket` to specify the socket path.
If you want to simply use it as the command `cloudnet`, you can add an alias to your `~/.bashrc`:
````shell
alias cloudnet='java -jar /home/cloudnet/cloudnet-client.jar'
````
