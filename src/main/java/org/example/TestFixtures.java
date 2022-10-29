package org.example;

import com.microsoft.playwright.*;
import io.github.cdimascio.dotenv.Dotenv;
import org.example.utils.CryptoText;
import org.junit.jupiter.api.*;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestFixtures {

    String fileName;
    Playwright playwright;
    Browser browser;

    Locators locators = new Locators();

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
        page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get(fileName)));
        context.close();
    }
}

class Invoices extends TestFixtures {

    LocalDate today = LocalDate.now();
    private static final Dotenv dotenv = Dotenv.configure()
            .directory("src/test/resources/.env")
            .ignoreIfMalformed()
            .ignoreIfMissing()
            .load();
    private final String fakturowaniaUserName = dotenv.get("FAKTUROWNIA_USER_NAME");
    private final String fakturowaniaPassword = dotenv.get("FAKTUROWNIA_PASSWORD");
    private final String pkoUserName = dotenv.get("PKO_USER_NAME");
    private final String pkoPassword = dotenv.get("PKO_PASSWORD");
    private final String toyotaUserName = dotenv.get("TOYOTA_USER_NAME");
    private final String toyotaPassword = dotenv.get("TOYOTA_PASSWORD");
    private final String tMobileUserName = dotenv.get("T_MOBILE_USER_NAME");
    private final String tMobilePassword = dotenv.get("T_MOBILE_PASSWORD");
    private final String laseLinkPhone = dotenv.get("LEASELINK_USER_NAME");
    private final String PATH_TO_DROPBOX = dotenv.get("PATH_TO_DROPBOX") + today.toString().substring(0, 7) + "\\";

    @Test
    void fakturownia() {
        page.navigate("https://fakturownia.pl/");
        page.locator(locators.getLoginButtonLocator()).click();
        assert fakturowaniaUserName != null;
        page.locator(locators.getUserNameLocator()).fill(CryptoText.decodeDES(fakturowaniaUserName));
        assert fakturowaniaPassword != null;
        page.locator(locators.getPasswordLocator()).fill(CryptoText.decodeDES(fakturowaniaPassword));
        page.locator(locators.getSubmitButtonLocator()).click();
        assertEquals("https://sopim.fakturownia.pl/", page.url());
        page.getByText("Przychody ").click();
        page.locator(locators.getMenuItemInvoicesLocator()).first().click();
        page.locator(locators.getTotalSumLocator()).waitFor();
        Locator rowLocator = page.locator(locators.getInvoicesColumnTableLocators());
        List<String> invoicesNumbers = rowLocator.allTextContents();
        int month = 9;
        int year = 2022;
        for (String nr : invoicesNumbers) {
            int monthSubStr = Integer.parseInt(nr.substring(7, 9));
            int yearSubStr = Integer.parseInt(nr.substring(2, 6));
            if (monthSubStr == month && yearSubStr == year) {
                System.out.println("Pobieram FV nr: " + nr);
                page.locator(String.format(locators.getCogIconLocator(), nr)).last().click();
                Download download = page.waitForDownload(() -> page.locator(String.format(locators.getDownloadLocator(), nr)).click());
                fileName = "FV" + year + "-" + month + "-" + nr.substring(10) + ".pdf";
                download.saveAs(Paths.get(PATH_TO_DROPBOX + fileName));
                System.out.println("Pobieram pliki do scieżki: " + fileName);
            }
        }
    }

    @Test
    void pko() {
        page.navigate("https://portal.pkoleasing.pl/Common/Authentication/Login");
        assert pkoUserName != null;
        page.locator("id=Login").fill(CryptoText.decodeDES(pkoUserName));
        assert pkoPassword != null;
        page.locator("id=Password").fill(CryptoText.decodeDES(pkoPassword));
        page.locator("id=Password").press("Enter");
        Locator invoices = page.getByText("Faktury").first();
        invoices.waitFor();
        Download download = page.waitForDownload(() -> page.locator("//tr[1]//td[3]//span[contains(@class,'pko-icon-pobierz_PDF')]").click());
        String invoiceName = page.locator("//tr[1]//td[1]//a").innerText();
        fileName = "PKO_LM-" + invoiceName.substring(3, 5) + "-" + invoiceName.substring(6, 8) + "-" + invoiceName.substring(9) + ".pdf";
        download.saveAs(Paths.get(PATH_TO_DROPBOX + fileName));
        System.out.println("Pobrano plik: " + fileName);
    }

    @Test
    void toyota() {
        page.navigate("https://portal.toyotaleasing.pl/Login");
        if (page.locator("//span[text()='Akceptuję wszystkie']").isVisible()) {
            page.locator("//span[text()='Akceptuję wszystkie']").click();
        }
        assert toyotaUserName != null;
        page.locator("id=login_layout_txtUName").fill(CryptoText.decodeDES(toyotaUserName));
        assert toyotaPassword != null;
        page.locator("id=login_layout_txtPName").fill(CryptoText.decodeDES(toyotaPassword));
        page.locator("//span[@class='dx-vam' and text()='ZALOGUJ']").click();
        Locator allInvoicesButton = page.locator("//span[@class='dx-vam' and text()='Wszystkie faktury']");
        allInvoicesButton.waitFor();
        allInvoicesButton.click();
        Locator InvoiceNumber = page.locator("//tr[contains(@id,'DXDataRow0')]//td[2]");
        InvoiceNumber.waitFor();
        String invoiceName = InvoiceNumber.innerText();
        Download download = page.waitForDownload(() -> page.locator("//tr[contains(@id,'DXDataRow0')]//td/a[contains(@class,'fa-file-pdf')]").click());
        fileName = "Toyota_" + invoiceName.substring(0, 5) + "-" + invoiceName.substring(6, 8) + "-" + invoiceName.substring(6, 8) + invoiceName.substring(9, 13) + ".pdf";
        download.saveAs(Paths.get(PATH_TO_DROPBOX + fileName));
        System.out.println("Pobrano plik: " + fileName);
    }

    @Test
    void tMobile() {

        Scanner scanner = new Scanner(System.in);

        page.navigate("https://nowymoj.t-mobile.pl/");
        if (page.locator("//button/span[text()='Accept all']").isVisible()) {
            page.locator("//button/span[text()='Accept all']").click();
        }
        if (page.locator("//button[contains(text(),'Ok')]").isVisible()) {
            page.locator("//button[contains(text(),'Ok')]").click();
        }
        assert tMobileUserName != null;
        page.locator("id=email").fill(CryptoText.decodeDES(tMobileUserName));
        page.locator("//button[text()='Dalej']").click();
        assert tMobilePassword != null;
        page.locator("id=password").fill(CryptoText.decodeDES(tMobilePassword));
        page.locator("//input[@value='Zaloguj się']").click();

        System.out.println("Podaj kod otrzymany SMS'em od T-mobile: ");
        String SmsCode = scanner.nextLine();

        page.locator("//input[@id='otpInput']").type(SmsCode);
        page.locator("//input[@id='submit1']").click();
        page.locator("//button[contains(text(),'Ok')]").click();
        Locator menuItem = page.locator("//li//span[contains(text(),'Płatności i faktury')]");
        menuItem.waitFor();
        menuItem.click();
        page.locator("//a[contains(@class,'secondary-button-black-md') and text()='Zobacz faktury']").click();
        Locator InvoiceNumber = page.locator("//li[1]//li[1]//div[@class='label']/span[2]");
        InvoiceNumber.waitFor();
        String invoiceName = InvoiceNumber.innerText();
        fileName = "T-Mobile_" + invoiceName + ".pdf";
        Download download = page.waitForDownload(() -> page.locator("//ul/li[1]//li[1]//a[contains(text(),'pobierz')]").click());
        download.saveAs(Paths.get(PATH_TO_DROPBOX + fileName));
        System.out.println("Pobrano plik: " + fileName);
    }

    @Test
    void leaseLink() {
        Scanner scanner = new Scanner(System.in);
        page.navigate("https://portal.leaselink.pl/");
        assert laseLinkPhone != null;
        page.locator("id=CallbackPanel_txtPhoneNumber").fill(CryptoText.decodeDES(laseLinkPhone));
        if (page.locator("id=cookiescript_accept").isVisible()) {
            page.locator("id=cookiescript_accept").click();
        }
        page.locator("id=CallbackPanel_btnPin").click();
        System.out.println("Podaj kod otrzymany SMS'em od LeaseLink: ");
        String SmsCode = scanner.nextLine();
        page.locator("id=CallbackPanel_txtPinNumber").fill(SmsCode);
        page.locator("id=CallbackPanel_btnLogin").click();
        Locator logoPortal = page.locator("id=divLogoPortal");
        logoPortal.waitFor();
        Locator InvoiceNumber = page.locator("//tr[contains(@id,'grdFaktury_DXDataRow0')]/td[2]");
        InvoiceNumber.waitFor();
        String invoiceName = InvoiceNumber.innerText();
        fileName = "Leaselink_" + invoiceName.replace("/", "-") + ".pdf";
        Download download = page.waitForDownload(() -> page.locator("//tr[contains(@id,'grdFaktury_DXDataRow0')]//a[contains(@class,'fa-file-pdf')]").click());
        download.saveAs(Paths.get(PATH_TO_DROPBOX + fileName));
        System.out.println("Pobrano plik: " + fileName);
    }
}

