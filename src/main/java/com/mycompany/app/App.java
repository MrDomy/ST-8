package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.BufferedReader;
import java.io.File;
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

    // Вспомогательный метод для настройки браузера (устанавливает папку для скачивания)
    private static ChromeOptions createBrowserOptions(String downloadDirPath) {
        Map<String, Object> preferences = new HashMap<>();
        preferences.put("download.default_directory", downloadDirPath);
        preferences.put("download.prompt_for_download", false);
        preferences.put("plugins.always_open_pdf_externally", true);

        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.setExperimentalOption("prefs", preferences);
        return chromeOptions;
    }

    public static void main(String[] args) {
        // Определяем пути к данным и результату
        Path sourceDataPath = Paths.get("data", "data.txt");
        Path outputDirPath = Paths.get("result").toAbsolutePath();

        List<String> cdInfo = new ArrayList<>();

        // Читаем файл с данными через BufferedReader
        try (BufferedReader reader = new BufferedReader(new FileReader(sourceDataPath.toFile()))) {
            String currentLine;
            while ((currentLine = reader.readLine()) != null) {
                cdInfo.add(currentLine.trim());
            }
        } catch (IOException e) {
            System.err.println("Ошибка при чтении файла data.txt: " + e.getMessage());
            return;
        }

        if (cdInfo.size() < 2) {
            System.out.println("Недостаточно данных в файле data.txt");
            return;
        }

        String artistName = cdInfo.get(0);
        String albumTitle = cdInfo.get(1);

        try {
            // Убеждаемся, что папка result существует
            Files.createDirectories(outputDirPath);

            // Удаляем старые PDF файлы, если они есть
            File finalPdfFile = new File(outputDirPath.toFile(), "cd.pdf");
            if (finalPdfFile.exists()) finalPdfFile.delete();
            File tempDownloadedPdf = new File(outputDirPath.toFile(), "papercdcase.pdf");
            if (tempDownloadedPdf.exists()) tempDownloadedPdf.delete();

            // Инициализация WebDriver с заданными настройками
            ChromeOptions browserConfig = createBrowserOptions(outputDirPath.toString());
            WebDriver webDriver = new ChromeDriver(browserConfig);

            try {
                // Открываем целевую страницу
                webDriver.get("http://www.papercdcase.com/");

                // Заполнение полей формы с использованием XPath
                WebElement cdArtistElem = webDriver.findElement(By.xpath("//input[@name='artist']"));
                cdArtistElem.sendKeys(artistName);

                WebElement albumTitleElem = webDriver.findElement(By.xpath("//input[@name='title']"));
                albumTitleElem.sendKeys(albumTitle);

                // Заполнение списка треков
                int trackIdx = 2;
                while (trackIdx < cdInfo.size() && trackIdx <= 17) {
                    int formTrackNumber = trackIdx - 1;
                    String xpathLocator = "//input[@name='track" + formTrackNumber + "']";
                    WebElement trackInputField = webDriver.findElement(By.xpath(xpathLocator));
                    trackInputField.sendKeys(cdInfo.get(trackIdx));
                    trackIdx++;
                }

                // Выбор переключателей (формат A4 и Jewel Case)
                WebElement paperFormatRadio = webDriver.findElement(By.xpath("//input[@value='a4']"));
                paperFormatRadio.click();

                WebElement caseTypeRadio = webDriver.findElement(By.xpath("//input[@value='jewel']"));
                caseTypeRadio.click();

                // Отправка формы (генерация обложки)
                WebElement generateCoverBtn = webDriver.findElement(By.xpath("//input[@name='submit']"));
                generateCoverBtn.submit();

                // Ожидание загрузки сформированного PDF-файла
                File downloadDir = outputDirPath.toFile();
                File generatedPdfFile = null;
                
                int waitAttempts = 0;
                while (waitAttempts < 30) {
                    Thread.sleep(1000);
                    File[] dirFiles = downloadDir.listFiles((dir, name) -> name.endsWith(".pdf") && !name.equals("cd.pdf"));
                    if (dirFiles != null && dirFiles.length > 0) {
                        generatedPdfFile = dirFiles[0];
                        break;
                    }
                    waitAttempts++;
                }

                // Переименование полученного файла в cd.pdf
                if (generatedPdfFile != null) {
                    if (generatedPdfFile.renameTo(finalPdfFile)) {
                        System.out.println("Файл успешно скачан и сохранен как result/cd.pdf");
                    } else {
                        System.out.println("Не удалось переименовать скачанный файл.");
                    }
                } else {
                    System.out.println("Превышено время ожидания скачивания PDF.");
                }

            } finally {
                // Закрываем браузер
                webDriver.quit();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
