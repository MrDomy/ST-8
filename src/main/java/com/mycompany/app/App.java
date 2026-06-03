package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;

public class App {
    public static void main(String[] args) {
        Path dataPath = Paths.get("data", "data.txt");
        Path resultDir = Paths.get("result").toAbsolutePath();

        try {
            List<String> lines = Files.readAllLines(dataPath, StandardCharsets.UTF_8);
            if (lines.isEmpty()) {
                System.out.println("Data file is empty.");
                return;
            }

            String artist = lines.size() > 0 ? lines.get(0).trim() : "";
            String title = lines.size() > 1 ? lines.get(1).trim() : "";

            // Ensure the result directory exists
            Files.createDirectories(resultDir);

            // Clean up any old cd.pdf
            File targetFile = new File(resultDir.toFile(), "cd.pdf");
            if (targetFile.exists()) {
                targetFile.delete();
            }
            File defaultPdf = new File(resultDir.toFile(), "papercdcase.pdf");
            if (defaultPdf.exists()) {
                defaultPdf.delete();
            }

            // Setup ChromeOptions to download directly to the result directory
            ChromeOptions options = new ChromeOptions();
            Map<String, Object> prefs = new HashMap<>();
            prefs.put("download.default_directory", resultDir.toString());
            prefs.put("download.prompt_for_download", false);
            prefs.put("plugins.always_open_pdf_externally", true); // Ensure PDF is downloaded, not opened in viewer
            options.setExperimentalOption("prefs", prefs);

            WebDriver driver = new ChromeDriver(options);

            try {
                driver.get("http://www.papercdcase.com/");

                WebElement artistInput = driver.findElement(By.xpath("//input[@name='artist']"));
                artistInput.sendKeys(artist);

                WebElement titleInput = driver.findElement(By.xpath("//input[@name='title']"));
                titleInput.sendKeys(title);

                for (int i = 2; i < lines.size(); i++) {
                    int trackNumber = i - 1;
                    if (trackNumber > 16) break; // Maximum 16 tracks supported by the website
                    String trackXPath = "//input[@name='track" + trackNumber + "']";
                    WebElement trackInput = driver.findElement(By.xpath(trackXPath));
                    trackInput.sendKeys(lines.get(i).trim());
                }

                WebElement a4Radio = driver.findElement(By.xpath("//input[@value='a4']"));
                a4Radio.click();

                WebElement jewelRadio = driver.findElement(By.xpath("//input[@value='jewel']"));
                jewelRadio.click();

                WebElement submitBtn = driver.findElement(By.xpath("//input[@name='submit']"));
                submitBtn.submit(); // Submit the form to generate PDF

                // Wait for the file to be downloaded
                File dir = resultDir.toFile();
                File downloadedPdf = null;
                for (int attempt = 0; attempt < 30; attempt++) {
                    Thread.sleep(1000); // Check every second for up to 30 seconds
                    
                    File[] files = dir.listFiles((d, name) -> name.endsWith(".pdf") && !name.equals("cd.pdf"));
                    if (files != null && files.length > 0) {
                        downloadedPdf = files[0];
                        break;
                    }
                }

                if (downloadedPdf != null) {
                    boolean renamed = downloadedPdf.renameTo(targetFile);
                    if (renamed) {
                        System.out.println("PDF generated and saved to result/cd.pdf successfully.");
                    } else {
                        System.out.println("Downloaded PDF found but could not be renamed.");
                    }
                } else {
                    System.out.println("Error: Failed to download the PDF within the timeout.");
                }

            } finally {
                driver.quit();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
