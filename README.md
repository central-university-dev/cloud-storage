
# Cloud storage

Client-server application implementing cloud storage.

## Usage

### Server side
    run cloud.storage.server.Server.main(String[]) or the corresponding server.jar file with port argument

### Client side
    run cloud.storage.client.Client.main(String[]) or the corresponding client.jar file with host and port arguments
## Client commands

```bash
<> means that you are not signed in yet.
<username> means your username.
Available commands:
ping words...
	Server will reply with the same message
time
	Get the server time
signUp login password
	Try to sign up on the server with passed login and password.
signIn login password
	Try to sign in on the server with passed login and password.
signOut
	Sign out from server.
-----For signed in users-----
upload pathFrom pathTo
    Upload file from this computer by pathFrom to server by pathTo
download pathFrom pathTo
    Download file from server by pathFrom to this computer by pathTo
move pathFrom pathTo
    Move file in server from pathFrom to pathTo (also may be used to rename file)
-----For signed in users-----
exit
	Shutdown client
help
	Show this message
```


## Demo

https://disk.yandex.ru/i/LincVY5XyLya6g

## Authors

- [@Marlesss](https://www.github.com/Marlesss)

