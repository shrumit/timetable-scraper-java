# timetable-scraper-java

*Scraper/parser for the [UWO Undergraduate Academic Timetable](http://studentservices.uwo.ca/secure/timetables/mastertt/ttindex.cfm)*


## Usage

This tool is intended to be run manually from the IDE.

1. In Eclipse, go to File > Import > General > Projects from Folder. Select the cloned repository.

2. Right-click on project name > Properties > Java Compiler > Java compliance level = 1.8. 

3. Run `DownloadWebpages.java` to download every subject's webpage to a local folder

4. Run `Main.java` to scrape data from the downloaded webpages

## See also

Output from this program is consumed by [Western Timetable Generator](https://github.com/shrumit/Western-Timetable-Maker).
