# timetable-scraper-java

*Scraper/parser for the [UWO Undergraduate Academic Timetable](http://studentservices.uwo.ca/secure/timetables/mastertt/ttindex.cfm)*


## Usage

Container:

1. Run `docker build . -t timetable-scraper-java:latest` in the repo root.
2. Make a new directory somewhere and `cd` into it.
3. `docker run -it -v ${pwd}:/execution timetable-scraper-java:latest`
4. The program's outputs will be saved in the current dir.

## See also

Output from this program is consumed by [Western Timetable Generator](https://github.com/shrumit/Western-Timetable-Maker).
