# Camunda NETCONF Plugins

A set of extensions for the service task in Camunda to support NETCONF communication to NSO or any other NETCONF-enabled device.
It uses the open source [ANC library](https://github.com/cisco-ie/anx) to communicate over the NETCONF protocol.

For now 3 types of NETCONF-specific tasks are possible:

- NETCONF read configuration and operational data
- NETCONF edit configuration
- NETCONF call action

Subscribing to NETCONF notifications is also possible by using a configurable external subscriber which posts events to the Camunda process whenever a new notification arrives (part of another project).

Four other additional plugins which might be useful are also available and are provided as reference:

- Sending CLI command to devices using NSO live status NED feature
- REST/RESTCONF calls using the [Unirest](http://unirest.io/java.html) java library
- Post messages on a Kafka bus
- An SSH plugin

## Installation

The `mvn package` command should generate a jar file which has to be copied in the libs directory of Camunda (`/camunda/server/apache.../lib`).
The following additional required libraries need to be also present in the CLASSPATH - usually in the same tomcat library directory (see the versions in `pom.xml`).

- anc
- jsch
- unirest-java
- kafka-clients
- ganymed

There is also a JSON template file for the Camunda Modeler supporting all these plugins which
will provide specific templates for the Service Task (`/utils/bpmn_templates.json`).

## Usage

The Camunda plugins provides 7 different element templates for the service task:
![Camunda service task elements](.imgs/IMG1.png)

All plugins provide an execution variable back to the workflow process containing 3 properties:

- code - return code, depending on the plugin
- value - the returned value in case of success
- detail - detail of the error in vase it was not successful

### NETCONF/NSO plugins

#### Authentication

NETCONF/NSO connection credentials can be specified in 2 ways:

- Using execution variables: `nc_host`, `nc_port`, `nc_user`, and `nc_pass`

![Execution variables](.imgs/IMG2.png)

- Using predefined profiles in NETCONF-profiles.properties (file has to be put in the Camunda tomcat configuration directory). Using one profile or another can be specified
on each task which requires it  - not that the profile has priority over the execution variables
One set of entries in the NETCONF-profiles.properties looks like:

    nso1_host=localhost
    nso1_port=2022
    nso1_user=admin
    nso1_pass=FfcZ19THZTd270glcNVGUQ==

And to use it you will need to specify the nso1 as the profile name.

![Authentication profile](.imgs/IMG3.png)

Note that password is encrypted using the provided password encryption script (**utils/passgen.sh**) and the key from the script has to match the one from the PassCrypt class

Any other authentication mechanism can be implemented depending on the use-case requirements

### NETCONF - Read configuration and operational data

![NETCONF read](.imgs/IMG4.png)

Allows to make a NETCONF read request for any configuration or operational data.
Need to specify

- The XPath or the XML filter for retrieving the required data
- If operational data should be included or not
- The variable which will contain the returned configuration

Optionally the task can check if a specified string is contained in the returned result, and in this case the execution result returned in the
variable will be a boolean specifying if string is contained or not.

#### NETCONF - Edit configuration

![NETCONF edit configuration](.imgs/IMG5.png)

Send NETCONF edit-config change requests.

Need to specify:

- The XML payload containing the needed configuration
- The variable which will contain the success of applying the config

Please note that execution variables may be included in any of the fields like that: `${variableName}`

#### NETCONF - Call action

![NETCONF call action](.imgs/IMG6.png)

Calls a predefined NSO action

Need to specify:

- The XML payload containing the action to be called
- The variable which will contain the output of the action execution

#### NSO - Send CLI command (requires the Karajan NSO service)

![Send CLI command](.imgs/IMG8.png)

Sends a CLI command to the specified device over NSO. For this specific task the additional karajan NSO package is required. 
The NSO service makes an abstraction by receiving just the device name and, based on its type/NED uses the live status exec to send the
specified command to the device. For now cisco-ios, cisco-ios-xr, alu-sr, juniper-junos, redback-se are supported, but
the package can be extended to support other device types.

Need to specify:

- The device to send the command to
- If the command to be send should be in config mode or not
- The variable which will contain the result of execution

Optionally the task can check if a specified string is contained in the returned result, and in
this case the execution result returned in the variable will be a boolean specifying if string is
contained or not.

### Additional sample plugins

#### SSH - Send command or configuration

Send CLI commands to any SSH enabled server/devices. Has three ways of interacting with the
target server/device:

- Shell – send batch of shell commands and get the output

![Send batch of shell commands](.imgs/IMG9.png)

- Config – send batch of commands to a device and get the output

![Send batch of device commands](.imgs/IMG10.png)

- Terminal – work in interactive mode, e.g. expect a string and send a specific answer. 

![Work in terminal mode](.imgs/IMG11.png)

Please always identify the end of the execution and send an end to signal that the execution should end and the return value should be sent, otherwise the task will not wait for the last output.
If the output of a command needs to be included in the returned result, it needs to be specified by adding `||true` at the end of the command, otherwise the command will be executed but the ouptut will not be returned. Like:

    $> ||ls -al||true

Need to specify

- SSH connection details (server, port, user, pass)
- Type of command (shell, config, terminal)
- The command(s) to be sent
- The variable which will contain the result of execution

Optionally the task can check if a specified string is contained in the returned result, and in this case the execution result returned
in the variable will be a boolean specifying if string is contained or not. 

#### HTTP - REST call (Unirest)

![REST call](.imgs/IMG12.png)

Sample plugin for making HTTP REST calls based on the Unirest java library, where each http method can be specified
on a separate line - can be also very easy exported from Postman

#### Kafka - Post Message

![Kafka post message](.imgs/IMG13.png)

Sample plugin for posting a message to a Kafka bus.
Kafka address, port and topic need to be specified, as well as the message to be posted.
Note that the client ID is set as "Karajan" in the plugin code.

## Observations

Providing information to the plugins is done using execution variables and this might cause issues in Camunda if any of these variables have a length which bigger than 4000 characters. This is caused by the fact that by default the db schema used
by Camunda uses a 4000 character limit for the fields which contain the execution variable values.

Several options are available to overcome this limitation:

- Modify the db structure to allow longer variable values, for example if using mysql/mariadb as the Camunda db, these are the changes that need to be performed:

        alter table  ACT_HI_DETAIL MODIFY TEXT_ varchar(65535);
        alter table  ACT_HI_DETAIL MODIFY TEXT2_ varchar(65535);
        alter table  ACT_RU_VARIABLE MODIFY TEXT_ varchar(65535);
        alter table  ACT_RU_VARIABLE MODIFY TEXT2_ varchar(65535);
        alter table  ACT_HI_VARINST MODIFY TEXT_ varchar(65535);
        alter table  ACT_HI_VARINST MODIFY TEXT2_ varchar(65535);

    If however, using another db (like the default in-memory h2 used by Camunda) other specific steps need to be performed.

- Another option is to modify the plugins and to provide input as **field injections**

    ![Field injections](.imgs/IMG14.png)

    ...or as json objects, like we are doing with the return object (taskResult), where we have 1 object with 3 properties - code,value,detail
    When using JSON objects with properties Camunda saves the variable content in different tables/fields which do not have this limitation (BLOB).
    The problem in the case of both field injections and JSON objectsis that the specific templates for the properties pane will no longer work in our web UI workflow management tool and you need to deal with low level variable setting instead of predefined input fields for each type of plugin.
