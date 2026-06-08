package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {
    public static void main(String[] args) {
        // Устанавливаем путь к папке result
        Path resultFolder = Paths.get("result").toAbsolutePath().normalize();
        try { 
            Files.createDirectories(resultFolder); 
        } catch (Exception ignored) {
        }

        ChromeOptions options = new ChromeOptions();
        Map<String, Object> chromePrefs = new HashMap<>();
        chromePrefs.put("download.default_directory", resultFolder.toString());
        chromePrefs.put("download.prompt_for_download", false);
        chromePrefs.put("plugins.always_open_pdf_externally", true);
        options.setExperimentalOption("prefs", chromePrefs);

        WebDriver driver = new ChromeDriver(options);
        
        try {
            driver.get("http://www.papercdcase.com/");
            
            // Читаем данные
            Map<String, Object> cdInfo = parseTxtData();
            
            // Заполняем поля
            driver.findElement(By.name("artist")).sendKeys((String) cdInfo.get("artistName"));
            driver.findElement(By.name("title")).sendKeys((String) cdInfo.get("albumTitle"));

            @SuppressWarnings("unchecked")
            List<String> trackList = (List<String>) cdInfo.get("trackList");
            
            int maxTracks = Math.min(16, trackList.size());
            for (int k = 0; k < maxTracks; k++) {
                String xpathExpr;
                if (k < 8) {
                    xpathExpr = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[" + (k + 1) + "]/td[2]/input";
                } else {
                    xpathExpr = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[" + (k - 7) + "]/td[2]/input";
                }
                driver.findElement(By.xpath(xpathExpr)).sendKeys(trackList.get(k));
            }

            // Выбор формата и подтверждение
            driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[2]")).click();
            driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[2]")).click();
            driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/p/input")).click();

            // Ждем скачивания
            Thread.sleep(10000);

            // Ищем скачанный файл
            Path[] downloadedFiles = Files.list(resultFolder)
                                          .filter(file -> file.toString().endsWith(".pdf"))
                                          .toArray(Path[]::new);
            
            if (downloadedFiles.length > 0) {
                Path newestPdf = downloadedFiles[0];
                for (Path p : downloadedFiles) {
                    if (Files.getLastModifiedTime(p).toMillis() > Files.getLastModifiedTime(newestPdf).toMillis()) {
                        newestPdf = p;
                    }
                }
                
                Path targetPdf = resultFolder.resolve("cd.pdf");
                if (Files.exists(targetPdf)) {
                    Files.delete(targetPdf);
                }
                
                Files.copy(newestPdf, targetPdf);
                Files.delete(newestPdf);
                System.out.println("Saved cd.pdf");
            } else {
                System.out.println("No PDF in " + resultFolder);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            driver.quit();
        }
    }

    private static Map<String, Object> parseTxtData() throws IOException {
        Map<String, Object> dataMap = new HashMap<>();
        List<String> tracks = new ArrayList<>();
        
        Path filePath = Paths.get("data", "data.txt").toAbsolutePath().normalize();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
            String currentLine;
            int lineNumber = 0;
            
            while ((currentLine = reader.readLine()) != null) {
                currentLine = currentLine.trim();
                if (currentLine.isEmpty()) continue;
                
                if (lineNumber == 0) {
                    dataMap.put("artistName", currentLine);
                } else if (lineNumber == 1) {
                    dataMap.put("albumTitle", currentLine);
                } else {
                    tracks.add(currentLine);
                }
                lineNumber++;
            }
        }
        
        dataMap.put("trackList", tracks);
        return dataMap;
    }
}
