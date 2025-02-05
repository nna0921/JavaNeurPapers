package nipscraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class scraper {
    private static final String BASE_URL = "https://papers.nips.cc/";
    private static final int THREAD_POOL_SIZE = 5; // Number of concurrent threads
    private static final String CSV_FILE = "papers_metadata.csv"; // CSV file name
    private static final int MAX_RETRIES = 3; // Number of retries before giving up
    private static final int TIMEOUT = 30000; // 30 seconds timeout for Jsoup connections
    private static final Random RANDOM = new Random(); // Random generator for delays

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        // Initialize CSV file with headers (only if it does not exist)
        try (PrintWriter writer = new PrintWriter(new FileWriter(CSV_FILE, true))) {
            File csvFile = new File(CSV_FILE);
            if (csvFile.length() == 0) {
                writer.println("Title,Authors,PDF Link,Year"); // CSV Header
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Document doc = fetchDocument(BASE_URL);
            if (doc == null) {
                System.err.println("Failed to load base URL");
                return;
            }

            Elements yearLinks = doc.select("a[href*=/paper_files/]");

            for (Element yearLink : yearLinks) {
                String yearUrl = BASE_URL + yearLink.attr("href");
                String year = yearUrl.replaceAll("\\D+", ""); // Extract year

                System.out.println("Processing year: " + year);
                executor.submit(() -> scrapeYearPage(yearUrl, year));
            }
        } finally {
            executor.shutdown();
        }
    }

    private static void scrapeYearPage(String yearUrl, String year) {
        try {
            Document yearDoc = fetchDocument(yearUrl);
            if (yearDoc == null) {
                System.err.println("Failed to load year page: " + yearUrl);
                return;
            }

            Elements paperLinks = yearDoc.select("a[href*=/paper/]");

            ExecutorService paperExecutor = Executors.newFixedThreadPool(3); // Separate thread pool for paper processing

            for (Element paperLink : paperLinks) {
                String paperUrl = BASE_URL + paperLink.attr("href");
                paperExecutor.submit(() -> scrapePaperPage(paperUrl, year));
                sleepRandom(); // Add delay to avoid rapid requests
            }

            paperExecutor.shutdown(); // Wait for all papers to be processed

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void scrapePaperPage(String paperUrl, String year) {
        try {
            Document paperDoc = fetchDocument(paperUrl);
            if (paperDoc == null) {
                System.err.println("Failed to load paper page: " + paperUrl);
                return;
            }

            // Extract Title
            Element titleElement = paperDoc.selectFirst("h4");
            String title = (titleElement != null) ? titleElement.text().trim() : "N/A";

            // Extract Authors
            Element authorHeader = paperDoc.selectFirst("h4:contains(Authors)");
            String authors = "N/A";
            if (authorHeader != null) {
                Element authorElement = authorHeader.nextElementSibling();
                if (authorElement != null) {
                    authors = authorElement.text().trim();
                }
            }

            // Extract PDF Link
            Element pdfButton = paperDoc.selectFirst("a[href$=.pdf]");
            String pdfUrl = (pdfButton != null) ? BASE_URL + pdfButton.attr("href") : "N/A";

            // Save metadata to CSV
            saveToCSV(title, authors, pdfUrl, year);

            // Download the PDF if available
            if (!pdfUrl.equals("N/A")) {
                String fileName = pdfUrl.substring(pdfUrl.lastIndexOf('/') + 1);
                downloadPDF(pdfUrl, "downloads/" + year + "/" + fileName);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static synchronized void saveToCSV(String title, String authors, String pdfUrl, String year) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(CSV_FILE, true))) {
            writer.printf("\"%s\",\"%s\",\"%s\",\"%s\"%n", title, authors, pdfUrl, year);
            System.out.println("Saved to CSV: " + title);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void downloadPDF(String fileURL, String savePath) {
        try {
            File pdfFile = new File(savePath);
            File parentDir = pdfFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs(); // Create directory if it does not exist
            }

            // Open connection and download the file
            try (BufferedInputStream in = new BufferedInputStream(new URL(fileURL).openStream());
                 FileOutputStream fileOutputStream = new FileOutputStream(pdfFile)) {

                byte[] dataBuffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                    fileOutputStream.write(dataBuffer, 0, bytesRead);
                }
                System.out.println("PDF saved: " + pdfFile.getAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Failed to download: " + fileURL);
            e.printStackTrace();
        }
    }

    /**
     * Fetches a document with retry logic, increased timeout, and User-Agent header.
     */
    private static Document fetchDocument(String url) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                System.out.println("Fetching: " + url + " (Attempt " + attempt + ")");
                Document doc = Jsoup.connect(url)
                        .timeout(TIMEOUT)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                        .referrer("https://www.google.com")
                        .get();
                System.gc();
                return doc;
            } catch (IOException e) {
                System.err.println("Failed attempt " + attempt + " for URL: " + url);
                if (attempt == MAX_RETRIES) {
                    System.err.println("Giving up on: " + url);
                }
                sleepRandom(); // Delay before retrying
            }
        }
        return null;
    }

    /**
     * Adds a random delay between 1 to 3 seconds to avoid server rate limiting.
     */
    private static void sleepRandom() {
        try {
            int delay = 1000 + RANDOM.nextInt(2000); // 1 to 3 seconds
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
