package org.example;

import lombok.Getter;

@Getter
public class Locators {
    private final String loginButtonLocator = "//a[@class='button-outline']/span[text()='Zaloguj się']";
    final String userNameLocator = "//*[@id='user_session_login']";
    final String passwordLocator = "//*[@id='user_session_password']";
    final String submitButtonLocator = "//input[@type='submit']";
    final String menuItemInvoicesLocator = "//li/a[contains(text(),'Faktury')]";
    final String totalSumLocator = "//td[@id='total_count']";
    final String invoicesColumnTableLocators = "//tr/td[2]//a";
    final String cogIconLocator = "//a[text()='%s']/../../..//span[@class='caret']";
    final String downloadLocator = "//a[text()='%s']/../../..//a[text()='Drukuj']";
}
