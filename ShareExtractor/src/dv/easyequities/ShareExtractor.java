package dv.easyequities;

import java.io.Console;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import dv.util.commandline.AnimatedLine;

public class ShareExtractor {	

	void extract() {
		boolean connected = false;
		WebElement password = null;
		WebDriver driver = null;

		while (!connected) {
			driver = connect();
			try {
				password = driver.findElement(By.id("Password"));
				connected = true;
			} catch (NoSuchElementException e) {
				System.out.println("timeout, retrying");
			}
		}
		WebElement user_name = driver.findElement(By
				.id("user-identifier-input"));
		WebElement login = driver.findElement(By
				.xpath("//button[@type='submit']"));
		Console c = System.console();

		// Enter user name
		String s_user_name = c.readLine("username: ");
		user_name.sendKeys(s_user_name);

		// Enter password
		String password_s = new String(System.console().readPassword("password: "));
		password.sendKeys(password_s);

		AnimatedLine.startThinking("loading share data");
		try {
			login.click();
		} finally {
			AnimatedLine.stopThinking();
		}

		List<WebElement> stock_names = driver.findElements(By
				.cssSelector(".stock-name.auto-ellipsis"));
		List<WebElement> num_shares = driver
				.findElements(By
						.cssSelector("table.table-columnsOnly.table-condensed.shares-fsr"));
		List<WebElement> purchase_cost = driver.findElements(By
				.cssSelector(".col-xs-6.right-value-column"));

		System.out.println("found " + stock_names.size() + " stocks");

		if (stock_names.size() != num_shares.size()
				&& stock_names.size() != purchase_cost.size() * 2) {
			System.err.println("cssSelector error: num shares does not match extracted share and/or price data");
			System.err.println("found " + num_shares.size() + " num_shares");
			System.err.println("found " + purchase_cost.size()
					+ " purchase_cost");
		}

		AnimatedLine.startThinking("parsing share data");
		try {
			String s = "";
			boolean first = true;
			for (int i = 0; i < num_shares.size(); i++) {
				if (first) {
					s+="\r\n(symbol,num shares,purchase price)\r\n";
					first = false;
				}

				String symbol = getStockSymbol(stock_names.get(i));
				String shares = getNumShares(num_shares.get(i));
				// every second element is the current value, while the other is the purchase price				
				String purchase_price = getPurchasePrice(purchase_cost.get(i * 2));
				String price_per_share = new BigDecimal(purchase_price).divide(new BigDecimal(shares),15,RoundingMode.HALF_UP).toPlainString();
				
				s+=symbol + "," + shares + "," + price_per_share+"\r\n";
			}
			AnimatedLine.stopThinking();
			System.out.println(s);
		} finally {
			AnimatedLine.stopThinking();
		}

		driver.quit();
	}

	private String getPurchasePrice(WebElement webElement) {
		return webElement.getText().replace(" ", "").substring(1).replace(".", "");
	}

	Pattern share_pattern = Pattern.compile("\\d+(\\.\\d+)?");

	private String getNumShares(WebElement webElement) {
		String shares_string = webElement.getText().replace(" ", "");
		Matcher m = share_pattern.matcher(shares_string);
		if (m.find())
			return m.group();
		return "";
	}

	private String getStockSymbol(WebElement webElement) {
		String result = webElement.getText();
		result = result.substring(result.indexOf('(') + 1, result.length() - 1);
		return "JSE:" + result;
	}

	private WebDriver connect() {
		// tell HtmlUnitDriver to shut its dirty mouth
		java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(
				Level.OFF);
		System.setProperty("org.apache.commons.logging.Log",
				"org.apache.commons.logging.impl.NoOpLog");

		WebDriver driver = new HtmlUnitDriver();
		String site = "https://www.easyequities.co.za/Account/SignIn";
		AnimatedLine.startThinking("loading " + site);
		try {
			driver.get(site);
		} finally {
			AnimatedLine.stopThinking();
		}
		return driver;
	}

	public static void main(String[] args) throws InterruptedException {
		new ShareExtractor().extract();
	}
}
