# timetable-scraper-java

*Scraper for the [UWO Undergraduate Academic Timetable](http://studentservices.uwo.ca/secure/timetables/mastertt/ttindex.cfm)*

## Usage

1. Run `DownloadWebpages.java` to download every subject webpage to a local folder
2. Run `Main.java` to scrape data from downloaded webpages

## Output

Output schema conforms to what is required by [Western Timetable Generator](https://github.com/shrumit/Western-Timetable-Maker), the only known consumer of this scraper. It should be fairly easy to modify it per your needs.

### search.json

```
[
	{
		id: ,
		text:
	}
]
```
### master.json
```
[
	{
		id: ,
		text: ,
		components: [
			{
				name:
				sections: [
					{
						name: ,
						timeslots: [
							{
								day: ,
								start: ,
								len: ,
								timebit: ,
								str1: ,
								str2: ,
								id:
							}
						]
					}
				]
			}
		]
	}
]
```

NB: This tool is designed for a one-time "snapshot" and should not be used for real-time/frequent data scraping.