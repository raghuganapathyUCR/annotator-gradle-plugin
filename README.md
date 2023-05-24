
# annotator-gradle-plugin

This project is a standalone gradle plugin that allows easy integration of AnnotatorScanner to any Java project.

  

_annotator-gradle-plugin is still under construction._

  

## Installation

  

### Local Installation - To build and host the plugin on mavenLocal()

 - Open a terminal and cd into this repository.
 - Run `./gradlew publishToMavenLocal`
 - In your target Java Project's `build.gradle` add the following
	 - In the `plugins{}` configuration block add `id "net.ltgt.annotator" version "0.0.1" `
	 - In the `repositories{}` configuration block add `mavenLocal()`
 -  In your target Java Project's `settings.gradle` add the following at the start of the file.
```
pluginManagement {  
	repositories {  
		gradlePluginPortal()  
		mavenLocal()  
	}  
}
```
 -	To pass the necessary `scanner.xml` config path, you can add it to the `annotator{}` configuration block.
### Example `build.gradle` for your Target Java Project 
```
plugins {  
	id 'java'  
	id "net.ltgt.errorprone" version "3.1.0"  
	id "net.ltgt.annotator" version "0.0.1"  
}  
  
group = 'org.example'  
version = '1.0-SNAPSHOT'  
  
repositories {  
	mavenCentral()  
	mavenLocal()  
}  
  
annotator{  
	configPath = "pathTo/scanner.xml"  
}  
  
dependencies {  
  
	compileOnly "com.google.code.findbugs:jsr305:3.0.2"  
	  
	errorprone "com.google.errorprone:error_prone_core:2.4.0"  
	errorproneJavac "com.google.errorprone:javac:9+181-r4173-1"  
}
```