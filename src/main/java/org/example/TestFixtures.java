package org.example;

import com.microsoft.playwright.*;
import io.github.cdimascio.dotenv.Dotenv;
import org.example.utils.CryptoText;
import org.junit.jupiter.api.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestFixtures {
    // Shared between all tests in the class.
    Playwright playwright;
    Browser browser;

    @BeforeAll
    void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.firefox().launch(new BrowserType.LaunchOptions().setHeadless(true).setSlowMo(50));
    }

    @AfterAll
    void closeBrowser() {
        playwright.close();
    }

    // New instance for each test method.
    BrowserContext context;
    Page page;

    @BeforeEach
    void createContextAndPage() {
        context = browser.newContext();
        page = context.newPage();
    }

    @AfterEach
    void closeContext() {
        page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("example.png")));
        context.close();
    }
}

class Test1 extends TestFixtures {
    private static final Dotenv dotenv = Dotenv.configure()
            .directory("src/test/resources/.env")
            .ignoreIfMalformed()
            .ignoreIfMissing()
            .load();
    private final String userName = dotenv.get("FAKTUROWNIA_USER_NAME");
    private final String password = dotenv.get("FAKTUROWNIA_PASSWORD");

    private Locator menuTabBar(String tagName) {
        return page.getByText(tagName);
    }

    @Test
    void fakturownia() {
        page.navigate("https://fakturownia.pl/");
        page.locator("//a[@class='button-outline']/span[text()='Zaloguj się']").click();
        assert userName != null;
        page.locator("//*[@id='user_session_login']").fill(CryptoText.decodeDES(userName));
        assert password != null;
        page.locator("//*[@id='user_session_password']").fill(CryptoText.decodeDES(password));
        page.locator("//input[@type='submit']").click();
        assertEquals("https://sopim.fakturownia.pl/", page.url());
        menuTabBar("Przychody ").click();
        page.locator("//li/a[contains(text(),'Faktury')]").first().click();
        page.locator("//td[@id='total_count']").waitFor();
        Locator rowLocator = page.locator("//tr/td[2]//a");
        List<String> texts2 = rowLocator.allTextContents();
        int month = 9;
        int year = 2022;
        for (String nr : texts2) {
            int monthSubStr = Integer.parseInt(nr.substring(7, 9));
            int yearSubStr = Integer.parseInt(nr.substring(2, 6));
            if (monthSubStr == month && yearSubStr == year) {
                System.out.println("Pobieram FV nr: " + nr);
                page.locator(String.format("//a[text()='%s']/../../..//span[@class='caret']", nr)).last().click();
                Download download = page.waitForDownload(() -> {
                    page.locator(String.format("//a[text()='%s']/../../..//a[text()='Drukuj']", nr)).click();
                });
                String fileName = "FV/FV" + year + "-" + month + "-" + nr.substring(10) + ".pdf";
                download.saveAs(Paths.get(fileName));
                System.out.println("Pobieram pliki do scieżki: " + fileName);
            }
        }
    }
}

