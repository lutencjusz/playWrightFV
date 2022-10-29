package org.example;

import com.microsoft.playwright.*;
import io.github.cdimascio.dotenv.Dotenv;
import org.example.utils.CryptoText;
import org.junit.jupiter.api.*;

import java.nio.file.Paths;

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
        browser = playwright.firefox().launch(new BrowserType.LaunchOptions().setHeadless(false).setSlowMo(50));
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

    }
}

