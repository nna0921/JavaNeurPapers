JavaNeurPapers Scraper

The JavaNeurPapers Scraper is a Java-based web scraper that extracts metadata and PDFs of research papers from the NeurIPS conference website. The scraper utilizes JSoup for web scraping and multithreading for efficient data collection.

#Features

Scrapes research paper metadata (title, authors, year, and PDF links).

Saves metadata into a CSV file (papers_metadata.csv).

Downloads PDFs of the papers into a structured folder hierarchy (downloads/{year}/).

Uses multi-threading to speed up the scraping process.

Implements retry logic for failed requests.

Adds random delays to avoid detection and IP bans.

#Technologies Used

Java 8+
JSoup (for web scraping)
ExecutorService (for multithreading)

Installation & Setup
1. Clone the Repository
git clone https://github.com/nna0921/JavaNeurPapers.git
cd JavaNeurPapers
2. Install Dependencies
Make sure you have Java 8+ installed. You can download it from OpenJDK or Oracle JDK.

#working of code

It extracts links to different years' paper collections.

It then visits each year’s page and fetches links to individual papers.

For each paper, it extracts metadata (title, authors, PDF link) and saves it to papers_metadata.csv.

If a PDF is available, it downloads it and organizes it into downloads/{year}/.

Output
CSV File (papers_metadata.csv) 
Format:
Title	Authors	PDF_Link	Year

Configuration
Modify the number of concurrent threads: Adjust THREAD_POOL_SIZE in scraper.java to change the concurrency level.

Change timeout settings: Modify TIMEOUT to set the maximum waiting time for HTTP requests.

Set the number of retries: Update MAX_RETRIES to determine how many times a request should be retried if it fails.

Error Handling
If a request fails, it retries up to MAX_RETRIES times.

If a document fails to load, it logs an error and moves to the next.

Random delays (1-3 seconds) are introduced to avoid server bans.

Author
Developed by nna0921 |Anna Zubair
Happy Scraping! 🚀

