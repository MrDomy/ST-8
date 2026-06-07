package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

public class App {

    public static void main(String[] args) {
        // Директории для входных данных и результатов
        Path fileDataPath = Paths.get("data", "data.txt");
        Path resultDirectory = Paths.get("result").toAbsolutePath();

        try {
            // Чтение данных из файла data.txt
            List<String> rawData = Files.readAllLines(fileDataPath);
            if (rawData.size() < 2) {
                System.out.println("Ошибка: недостаточно данных в data.txt");
                return;
            }
            String bandName = rawData.get(0).trim();
            String cdTitle = rawData.get(1).trim();

            // Подготовка директории и очистка
            if (!Files.exists(resultDirectory)) {
                Files.createDirectories(resultDirectory);
            }
            File previousPdf = new File(resultDirectory.toFile(), "cd.pdf");
            if (previousPdf.exists()) {
                previousPdf.delete();
            }

            // Настройка ChromeOptions для автоматического скачивания PDF
            HashMap<String, Object> chromePrefs = new HashMap<>();
            chromePrefs.put("download.default_directory", resultDirectory.toString());
            chromePrefs.put("download.prompt_for_download", false);
            chromePrefs.put("plugins.always_open_pdf_externally", true);
            ChromeOptions browserOptions = new ChromeOptions();
            browserOptions.setExperimentalOption("prefs", chromePrefs);

            WebDriver webDriver = new ChromeDriver(browserOptions);

            try {
                webDriver.get("http://www.papercdcase.com/index.php");

                // Заполнение полей Исполнитель и Альбом по name
                webDriver.findElement(By.name("artist")).sendKeys(bandName);
                webDriver.findElement(By.name("title")).sendKeys(cdTitle);

                // Заполнение списка треков с использованием абсолютных XPath из задания
                for (int index = 2; index < rawData.size(); index++) {
                    int trackPosition = index - 1;
                    if (trackPosition > 16) break; // Максимум 16 треков

                    String trackAbsolutePath;
                    if (trackPosition <= 8) {
                        trackAbsolutePath = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[" + trackPosition + "]/td[2]/input";
                    } else {
                        trackAbsolutePath = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[" + (trackPosition - 8) + "]/td[2]/input";
                    }
                    webDriver.findElement(By.xpath(trackAbsolutePath)).sendKeys(rawData.get(index).trim());
                }

                // Клик по радиокнопкам (Jewel case и A4) по абсолютным путям
                webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[2]")).click();
                webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[2]")).click();
                
                // Клик по кнопке генерации
                webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/p/input")).click();

                // Ожидание скачивания (даем браузеру время)
                Thread.sleep(8000);

                // Поиск скачанного файла и его переименование
                File[] downloadedFiles = resultDirectory.toFile().listFiles((dir, name) -> name.endsWith(".pdf") && !name.equals("cd.pdf"));
                
                if (downloadedFiles != null && downloadedFiles.length > 0) {
                    // Берем первый попавшийся PDF файл
                    File freshlyDownloaded = downloadedFiles[0];
                    // Если их несколько, ищем самый новый
                    for (File f : downloadedFiles) {
                        if (f.lastModified() > freshlyDownloaded.lastModified()) {
                            freshlyDownloaded = f;
                        }
                    }
                    
                    File finalDestination = new File(resultDirectory.toFile(), "cd.pdf");
                    if (freshlyDownloaded.renameTo(finalDestination)) {
                        System.out.println("Готово: файл скачан и сохранен как result/cd.pdf");
                    }
                } else {
                    System.out.println("Не удалось обнаружить скачанный PDF-файл в папке result.");
                }

            } finally {
                webDriver.quit();
            }

        } catch (Exception ex) {
            System.err.println("Произошла системная ошибка: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
