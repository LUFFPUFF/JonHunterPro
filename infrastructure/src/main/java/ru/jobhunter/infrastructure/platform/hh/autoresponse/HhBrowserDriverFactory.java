package ru.jobhunter.infrastructure.platform.hh.autoresponse;

import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Objects;

@Component
public class HhBrowserDriverFactory {

    private final HhBrowserAutoResponseProperties properties;

    public HhBrowserDriverFactory(
            HhBrowserAutoResponseProperties properties
    ) {
        this.properties = Objects.requireNonNull(
                properties,
                "HH browser auto response properties must not be null"
        );
    }

    public WebDriver createDriver() {
        ChromeOptions options = new ChromeOptions();

        options.setPageLoadStrategy(PageLoadStrategy.EAGER);

        Path userDataDir = Path.of(properties.requireUserDataDir())
                .toAbsolutePath()
                .normalize();

        options.addArguments("--user-data-dir=" + userDataDir);
        options.addArguments("--profile-directory=Default");
        options.addArguments("--start-maximized");
        options.addArguments("--disable-notifications");

        if (properties.headless()) {
            options.addArguments("--headless=new");
            options.addArguments("--window-size=1600,1000");
        }

        WebDriver driver = new ChromeDriver(options);

        driver.manage()
                .timeouts()
                .pageLoadTimeout(properties.waitTimeout());

        return driver;
    }
}
