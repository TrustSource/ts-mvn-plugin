# ecs-mvn-plugin
TrustSource (https://www.trustsource.io) Code Scan maven plugin. A plugin for maven to transfer dependency information to TrustSource service to allow dependency checking, vulnerability and license analysis. 

[![Build Status](https://travis-ci.org/eacg-gmbh/ecs-mvn-plugin.svg?branch=master)](https://travis-ci.org/eacg-gmbh/ecs-mvn-plugin)
[![Maven](https://img.shields.io/maven-central/v/de.eacg/ecs-mvn-plugin.svg)](http://search.maven.org/#search|gav|1|g%3A%22de.eacg%22%20AND%20a%3A%22ecs-mvn-plugin%22)
[![MIT License](https://img.shields.io/npm/l/check-dependencies.svg?style=flat-square)](http://opensource.org/licenses/MIT)

## System requirements

- JDK
    - 1.7 or later
- Maven
    - 3.0 or later
    
## Quick installation

It is pretty simple to include the TrustSource scan into your existing Maven projects. Mayke use of the ecs-mvn-plugin by declaring it in your pom and using the given example as template.
Then configure the plugin with your security credentials and bind it to the maven install lifecycle.

To retrieve a TrustSource ApiKey, login to the TrustSource web application at https://app.trustsource.io. Accounts can be obtain upon subscription. A free version is available. After registration go to profile settings by clicking the gear-icon in the navigation bar and copy one of your company’s ApiKeys to your clipboard. Paste this value between the `<apiKey>...</apiKey>` tags of your project’s pom.xml file. Enter your TrustSource username, probably your e-mail address, between the `<userName>...</userName>` tags and find a reasonable project name to enter it between the `<projectName>...</projectName>` tags.

Simple example pom.xml:
```xml
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
        <!-- Scan dependencies and transfer to the server -->
        <execution>
          <id>dependency-scan</id>
          <phase>install</phase>
          <goals>
            <goal>dependency-scan</goal>
          </goals>
        </execution>
        <!-- Check dependencies for legal issues and vulnerabilities during the build -->
        <execution>
          <id>dependency-check</id>
          <phase>compile</phase>
          <goals>
            <goal>dependency-check</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```
Execute on command line:
``
mvn clean install
``
## Advanced Setup
Multi module reactor build

If you use a more complex, maven multi module setup, you may define the ecs-mvn-plugin in the `<pluginManagement>` section of your module pom. All your children projects inherit this definition.

TrustSource requires unique project names. Therefore you have to define the project name and moduleId in every child-modules pom within the `<configuration>`-tag.
Alternatively, split your project name in 2 parts, the first part contains your projects main name and the second part is dynamically resolved for every sub-module you want to scan. Like so:

```xml
<projectName>My new cool Project-${project.name}</projectName>
```


Now simply apply the plugin to one or more child modules by including the following lines in the `<build>` element of the pom.

pom.xml for child modules
```
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
```
Export your credentials to a property file

If you do not want to include your sensitive credentials in the pom, which may be managed by a version control system, store this information in a separate file. This file may be for example located in your user home directory and should have json data format. If you externalize your security credentials, they are reusable for different projects, even if this projects utilize different build tools.

properties file ‘ecs-settings.json’ in your home directory:
```json
{
    "userName": "email@yourdomain.com",
    "apiKey": "234434-fb9b-46f2-db84-ec3f57a46f2"
}
```
Afterwards simply adjust the configuration of the ecs-mvn-plugin by specifying an additional `<credentials>` element. In the element define the path to your properties file and ecs-mvn-plugin will then read the properties from this file. The tilde, ‘~’, represents your user home directory, the dot, ‘.’ stands for the current working directory and forward slashes ‘/’ are used to separate subdirectories.

configuration of the ecs-mvn-plugin:
```xml
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
```

## Reference
ecs-maven-plugin provides two goals that can be executed within different lifecycles  
 - `dependency-scan` - scans dependencies of the projects and uploads them to the server
 - `dependency-check` - checks dependencies of the projects for legal issues and vulnerabilities. This goal also allows to break a build if some issues and vulnerabilities are found during a scan.
 
 *NOTE*: Before executing `dependency-check` goal first time, it is required to execute `dependency-scan` in order to transfer the information about the project and its modules and configure the legal settings of every module in the [TrustSource web application](https://app.trustsource.io). 
 
### Additional settings

If you do not want to transfer the detected dependency information for every maven install call, bind the ecs-mvn-plugin for example to the deploy lifecycle. If you haven’t configured the `<distributionManagement>` element in your poms, because you prefer your own style distribution, then you have to disable the maven distribution plugin to prevent error messages while invoking the deploy lifecycle phase (mvn deploy).

Disable the maven deployment plugin:
```
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-deploy-plugin</artifactId>
  <version>2.8.2</version>
  <configuration>
    <skip>true</skip>
  </configuration>
</plugin>                
```

#### Configuration parameters

- *credentials* (Optional): Path to a json file, which holds ‘userName’ and ‘apiKey’ credentials. Use ‘~’ as shortcut to your home directory, and ‘.’ for the current working directory. A forward slash ‘/’ separates directories. *Default*: apiKey and userName are expected to be set in the plugin configuration

- *apiKey* (Required, if not specified in credentials file): This key permits the access to ECS server. Create or retrieve the key from your profile settings of the ECS web application.
        
- *userName* (Required, if not specified in credentials file.): Identifies the initiator of the data transfer.
    
- *projectName* (Required): For which project is the dependency information transferred.
    
- *skip* (Optional): Set to true do disable the ecs-mvn-plugin. *Default: false*
    
- *skipTransfer* (Optional): Set to true to execute a dry run and do not transfer anything. *Default: false*

- *allowBreakBuild* (Optional): Allow to break a build if legal issues or vulnerabilities are found. *Default: true*

- *breakOnLegalIssues* (Optional): Allow to break a build on legal issues. *Default: true*

- *breakOnVulnerabilities* (Optional): Allow to break a build on vulnerabilities. *Default: true*

- *breakOnViolationsOnly* (Optional): Allow to break on violations only. *Default: true*

- *breakOnViolationsAndWarning* (Optional): Allow to break on violations and warnings. *Default: false*

- *assumeComponentsModified* (Optional): By checking for legal issues, assume all components are modified *Default: false*
 


Query plugin help on command line:
``
mvn help:describe -Dplugin=de.eacg:ecs-mvn-plugin \
-Dgoal=dependency-scan -Ddetail=true
``

## License
[MIT](https://github.com/eacg-gmbh/ecs-mvn-plugin/blob/master/LICENSE)