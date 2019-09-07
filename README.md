## Personal web crawler and search engine.

### Requirements:
- Java 1.8
- Maven 3

### Build:
 `mvn clean package`
### Run:
`mvn spring-boot: run -Dspring.profiles.active=dev`

### Configuration:
all configuration files (.yaml) located in directory `/src/main/resoures/config`
Before you start the application, set the paths(absolute) to the directories where data will be stored.
Configuration example from my notebook (application-dev.yaml):
```
swsConfig:
    storage:
        crawlStorageRootDir: /storage/pwc/crawl
        luceneIndexRootDir: /storage/pwc/idx
        indexedUrlsFilePath: /storage/pwc/indexed.ser
```
### How it works.
On the main page type the URL of the site to be indexed. 


After crawling of 100 pages from given domain you will be redirected to the search page.


### Technologies used:
- [Web application - spring-boot](http://projects.spring.io/spring-boot/)
- [Web crawler - crawl4j](https://github.com/yasserg/crawler4j)
- [Indexer - lucene](https://lucene.apache.org/)
- [Frontend - Ractjs](https://reactjs.org/)
