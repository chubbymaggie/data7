# Data7 Project: An automatically generated Vulnerability dataset

## What is it?

Data7 is a tool that put together vulnerability report and vulnerability patches  of a given software project in an automated way under the form of a dataset. Once created the dataset can then easily be updated with the latest information available. The information that can be found in the dataset is the following:
    
* CVE number
* description
* CWE number (if applicable)
* time of creation 
* time of last modification
* CVSS severity score
* bugs ids (if existing)   
* list of impacted versions
* list of commits that fixed the vulnerability
    * commits contains:
        * hash
        * timestamp
        * message
        * fixes (files in their states before and after fix)


## Why? 

When investigating a vulnerability, a security analyst need as much information as possible on it and usually reports are a good starting point. However, the most insightful piece of information on the vulnerability is usually the fix that was created to solve it. From the fix, the origin of the vulnerability and its type can be determined. Fixes when available can be found as separated links in the reports but it is far from being always the case. 
If linking fixes and reports by hand is possible, it is time consuming. So when the analysis of not one but many vulnerabilities is considered then it is not possible to do it by hand anymore. A good example of a case where the analysis of a large number of vulnerabilities is required is the creation of a Vulnerability Prediction Model. 

Thus, the link should be made automatically and not manually which is possible by cross-checking information from vulnerability report, bug trackers and versioning history and that's precisely what data7 is doing.

## Requirements

To create and update a dataset an internet connection is required. However nothing is required to read an existing data7.
Other dependencies are handled through maven.

## How does it work ?
For a given project P

* Creating a dataset
    1. Data7 will first connect to the NVD database and download all the XML feeds for vulnerabilities (2002-Current Year)
    2. Data7 will then parse all the XML and retrieve all vulnerabilities reported for P over the history
    3. For each vulnerability, all declared links are analysed and if a mention to a bug report is made or a link to a fix commit is present, they are saved
    4. The git repository of P is cloned in a local folder
    5. For each vulnerability that had a link to a fixing commit, all information on the commit are retrieved from the git repository and added to the one of the vulnerability
    6. For each commit in the versioning history that was not yet analysed in (v.), analyse the message and look for a bug id that was present in the report or for a CVE Identifier and if a match is made then commit information is added to the vulnerability information.


* Updating a dataset
    1. Data7 first check when the latest update occurred, if less than 7 days passed since the last update, only the modified and recent xml feed are downloaded, otherwise all xml feeds from years that have been modified since the last update are downloaded
    2. Data7 will then parse the XML and retrieve all vulnerabilities reported for P and create a new entry if there is a new vulnerability or update it if necessary
    3. For each updated or created vulnerability entry, look for a new link of commit fix and/or bug id.
    4. The git repository is pulled
    5. For all new/updated entry  that has a new link, retrieve the commit information
    6. For all entry that has a new bug id, check whether this bug id was already found in the commit history (from a previous crawl)
    7. For all latest commit (since latest update), analyse the message and look for a bug id that was present in the report or for a CVE Identifier and if a match is made then the commit information is added to the vulnerability information. 


## Dataset Structure

The dataset generated by data7 can be accessed in two ways (a third one is planned in the future) either through an API relying on a data7 Object that can be serialized or through an XML file.

### API (binary form)

![schema](doc/model.png)

When calling upon the creation or the update of a dataset, the user will receive a Data7 object. This object contains information on the project the dataset is based on (see next section), and the vulnerability dataset (VulnerabilitySet Object), other fields are present but only contain additive information required by the tool for update such as a list of bug id and their corresponding hash commit, a list of bug id to CVE identifiers, a list of all hash already processed and a list of found cve identifiers in commit for whose report is not yet available which can be interesting to investigate not yet disclosed vulnerabilities.

The vulnerabilitySet object is composed of the following information, a list of every vulnerability ever reported for the chosen project (list of Vulnerability Object) and the last time the dataset was updated last.

A Vulnerability Object entry has the following information integrated:

* CVE number
* description of the vulnerability in the report
* CWE number (if applicable)
* time of creation 
* time of last modification
* CVSS severity score
* bug ids (if existing, can be more than one)   
* list of impacted versions
* list of fixing commit (Commit object)

A Commit Object contains the hash, the message and the timestamp of the commit as well as the list of files that were modifier (FileFix Object).

A FileFix Object contains information on time the file was last modified before the given commit and its corresponding previous hash as well as the file in its state before and after commit (FileInterest Object).

A FileInterest Object contains the text of the file and its fullPath in the project.



### XML exporter

The dataset can as well be exported as an XML file that will only contain vulnerability with reported fixes.
The schema of the xml is the following:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
    <data7 last_updated="YYYY-MM-DD HH:mm:ss CEST" project="project name">
      <cve id="CVE-YYYY-XXXXXX" last_modified="timestamp">
        <cwe></cwe>
        <score></score>
        <description></description>
        <affectedVersions>
          <version></version>
        </affectedVersions>
        <bugs/>
        <patches>
          <commit hash="aaaaaaa" timestamp="xxxxxxx">
            <message></message>
            <files>
              <file>
                <before hash="aaaaaaaa" path="src/file.c">
                Content of the file
                </before>
                <after path="src/file.c">
                Content of the file
                </after>
              </file>
            </files>
          </commit>
        </patches>
      </cve>
    </data7>
```                

### Extending to other tools/database

As analyst might want to import the dataset to their own tool or favorite database, a last option is possible that allow the user to import the data at the same time the dataset is being built/update through an Observer. Indeed, when asking to update/create a dataset the user can pass as an argument a listener that implements the DatasetUpdateListener interface that will notify of the latest update in live.

The interface offer 6 notifications:

* when a bug is added to a vulnerability
* when a commit is added to a vulnerability
* when a score is updated (CVSS)
* when a cwe is updated
* when a new vulnerability is added
* when the update is finished

and is presented as follow:

```java
package data7.importer.cve;

import data7.model.vulnerability.Vulnerability;

import java.util.EventListener;

public interface DatasetUpdateListener extends EventListener {

    void bugAddedTo(Vulnerability vulnerability, String bugId);

    void commitAddedTo(Vulnerability vulnerability, String hash);

    void scoreUpdatedFor(Vulnerability vulnerability);

    void cweUpdatedFor(Vulnerability vulnerability);

    void addVulnerability(Vulnerability vulnerability);

    void updatefinished();
}
``` 

This interface allows to develop more functionality for the dataset such as the one currently in development in the graph section, which use a temporal graph database to store the dataset. Note that it might be better to first do a batch import and then use the interface when update are being done.

## Supported projects

Currently four open source projects are supported :

* Linux Kernel
* Wireshark
* OpenSSL
* SystemD

but it can easily be extended to any other project where it is possible to find the following information:

*  name of the project as it appears in NVD database, e.g, linux_kernel
*  url of a git remote repository, e.g, https://github.com/torvalds/linux
*  regular expression catching link to remote repository and hashes in it, e.g, .*?(github\\.com|git\\.kernel\\.org).*?(commit)+.*?(h\\=|/)+([a-f0-9]+)
*  url of a bug tracker, e.g, https://bugzilla.kernel.org/
*  regular expression catching link to bug tracker and bug id in it, e.g, .*(bugzilla\\.kernel\\.org).*?(id\\=)([0-9]+)
*  regular expression catching bug id in git commit message, e.g, .*(bugzilla\\.kernel\\.org).*?(id\\=)([0-9]+)

## How to use the tools



## How to integrate it to other tool?


