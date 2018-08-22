# ecs-mvn-plugin
EACG Code Scan maven plugin. A maven plugin to transfer dependency information to ECS server.

[![Build Status](https://travis-ci.org/eacg-gmbh/ecs-mvn-plugin.svg?branch=master)](https://travis-ci.org/eacg-gmbh/ecs-mvn-plugin)
[![Maven](https://img.shields.io/maven-central/v/de.eacg/ecs-mvn-plugin.svg)](http://search.maven.org/#search|gav|1|g%3A%22de.eacg%22%20AND%20a%3A%22ecs-mvn-plugin%22)
[![MIT License](https://img.shields.io/npm/l/check-dependencies.svg?style=flat-square)](http://opensource.org/licenses/MIT)

## Installation
The installation instructions will be provided as part of the [ECS web-application](https://ecs.eacg.de/install/).
## License
[MIT](https://github.com/eacg-gmbh/ecs-mvn-plugin/blob/master/LICENSE)



#Quick installation

It’s easy to include ECS into your existing Maven projects. Utilize the ecs-mvn-plugin by declaring it in your pom and using the given example as a template.
Then configure the plugin with your security credentials and bind it for example to the maven install lifecycle.

To retrieve the ECS ApiKey, login to the ECS web application. Goto profile settings by clicking the gear-icon in the navigation bar and copy one of your company’s ApiKeys to your clipboard. Paste this value between the <apiKey>...</apiKey> tags of your project’s pom.xml file. Enter your ECS username, probably your e-mail address, between the <userName>...</userName> tags and find a resonable projectname to enter it between the <projectName>...</projectName> tags.

Simple example pom.xml:
``
<build>
  <plugins>
    <plugin>
      <groupId>de.eacg</groupId>
      <artifactId>ecs-mvn-plugin</artifactId>
      <version>0.1.4</version>
      <configuration>
        <apiKey>YOUR API KEY GOES HERE</apiKey>
        <userName>LOGINNAME(e-mail) GOES HERE</userName>
        <projectName>THE NAME OF YOUR PROJECT</projectName>
      </configuration>
      <executions>
        <execution>
          <id>dependency-scan</id>
          <phase>install</phase>
          <goals>
            <goal>dependency-scan</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
``
Execute on command line:
``
mvn clean install
``
#Advanced Setup
Multi module reactor build

If you use a more complex, maven multi module setup, you may define the ecs-mvn-plugin in the <pluginManagement> section of your module pom. All your children projects inherit this definition.

ECS requires unique project names. Therefore you have to define the project name and moduleId in every child-modules pom within the <configuration>-tag.
Alternatively, split your project name in 2 parts, the first part contains your projects main name and the second part is dynamically resolved for every sub-
module you want to scan. Like so:

<projectName>My new cool Project-${project.name}</projectName>

Now simply apply the plugin to one or more child modules by including the following lines in the <build> element of the pom.

pom.xml for child modules
``
<build>
  <plugins>
    <plugin>
      <groupId>de.eacg</groupId>
      <artifactId>ecs-mvn-plugin</artifactId>
      <configuration>
        <projectName>unique project name</projectName>
      </configuration>
    </plugin>
  </plugins>
</build>
``
Export your credentials to a property file

If you do not want to include your sensitive credentials in the pom, which may be managed by a version control system, store this information in a separate file. This file may by for example located in your user home directory and should have json data format. If you externalize your security credentials, they are reusable for different projects, even if this projects utilize different build tools.

properties file ‘ecs-settings.json’ in your home directory:
``
{
    "userName": "email@yourdomain.com",
    "apiKey": "234434-fb9b-46f2-db84-ec3f57a46f2"
}
``
Afterwards simply adjust the configuration of the ecs-mvn-plugin by specifying an additional <credentials> element. In the element define the path to your properties file and ecs-mvn-plugin will then read the properties from this file. The tilde, ‘~’, represents your user home directory, the dot, ‘.’ stands for the current working directory and forward slashes ‘/’ are used to separate subdirectories.

configuration of the ecs-mvn-plugin:
``
<plugin>
    <groupId>de.eacg</groupId>
    <artifactId>ecs-mvn-plugin</artifactId>
    <version>0.1.4</version>
    <configuration>
        <credentials>~/ecs-settings.json</credentials>
        <projectName>THE NAME OF YOUR PROJECT</projectName>
    </configuration>
   ...
</plugin>
``
#Reference
##Other maven lifecycles

If you do not want to transfer the detected dependency information for every maven install call, bind the ecs-mvn-plugin for example to the deploy lifecycle. If you haven’t configured the <distributionManagement> element in your poms, because you prefer your own style distribution, then you have to disable the maven distribution plugin to prevent error messages while invoking the deploy lifecycle phase (mvn deploy).

Disable the maven deployment plugin:
``
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-deploy-plugin</artifactId>
  <version>2.8.2</version>
  <configuration>
    <skip>true</skip>
  </configuration>
</plugin>                
``
All relevant ecs-mvn-plugin configuration parameters

credentials
    Path to a json file, which holds ‘userName’ and ‘apiKey’ credentials. Use ‘~’ as shortcut to your home directory, and ‘.’ for the current working directory. A forward slash ‘/’ separates directories.
    Optional: default: apiKey and userName are expected to be set in the plugin configuration
apiKey
    This key permits the access to ECS server. Create or retrieve the key from your profile settings of the ECS web application.
    Required, if not specified in credentials file.
userName
    Identifies the initiator of the data transfer.
    Required, if not specified in credentials file.
projectName
    For which project is the dependency information transferred.
    Required
skip
    Set to true do disable the ecs-mvn-plugin.
    Optional: default: false
skipTransfer
    Set to true to execute a dry run and do not transfer anything.
    Optional: default: false

Query plugin help on command line:
``
mvn help:describe -Dplugin=de.eacg:ecs-mvn-plugin \
-Dgoal=dependency-scan -Ddetail=true
``
##System requirements

JDK
    1.7 or later
Maven
    3.0 or later

