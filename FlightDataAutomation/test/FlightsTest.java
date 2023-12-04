import io.netty.handler.codec.http.websocketx.extensions.WebSocketExtensionEncoder;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.*;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import org.openqa.selenium.JavascriptExecutor;

import static java.lang.Thread.sleep;

@RunWith(JUnitParamsRunner.class)
public class FlightsTest {
    private static WebDriver driver;
    private static Connection connection;

    @BeforeClass
    public static void setUp() {
        System.setProperty("webdriver.gecko.driver", "geckodriver");
        driver = new FirefoxDriver();
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:flights.sqlite");
            Statement statement = connection.createStatement();
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS flights (id INTEGER PRIMARY KEY AUTOINCREMENT, airline_name TEXT, price TEXT)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void performInitialSearch(String initialDepartureDate, String initialReturnDate, String destination, WebDriverWait wait) {
        WebElement departureInput = driver.findElement(By.cssSelector(".uNiB1 > div:nth-child(1) > div:nth-child(1) > div:nth-child(1) > div:nth-child(1) > input:nth-child(2)"));
        departureInput.clear();
        departureInput.sendKeys(initialDepartureDate);

        WebElement returnInput = driver.findElement(By.cssSelector(".uNiB1 > div:nth-child(1) > div:nth-child(1) > div:nth-child(2) > div:nth-child(1) > input:nth-child(1)"));
        returnInput.clear();
        returnInput.sendKeys(initialReturnDate);

        WebElement destinationInput = driver.findElement(By.cssSelector("div.WKeVIb:nth-child(1) > div:nth-child(1) > div:nth-child(1) > div:nth-child(1) > div:nth-child(1) > input:nth-child(1)"));
        destinationInput.clear();
        destinationInput.sendKeys(destination);

        WebElement firstSelection = wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("n4HaVc")));
        firstSelection.click();

        WebElement searchButton = driver.findElement(By.cssSelector(".TUT4y"));
        searchButton.click();

        // Wait for the page to load - adjust the expected conditions based on the actual behavior of the page
        wait.until(ExpectedConditions.urlContains("search"));

        storeFlightInformation(wait);
    }

    @Test
    @Parameters({"Cancun", "Las Vegas", "Denver", "Rome", "Milan", "Paris", "Madrid", "Amsterdam", "Singapore"})
    public void setupFlightSearch(String destination) throws ParseException, InterruptedException {
        driver.get("https://www.google.com/travel/flights");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            String startDate = "May 1, 2024";
            String endDate = "May 7, 2024";



            performInitialSearch(startDate,endDate,destination,wait);



            // Perform search with the given departure and return date
            performFlightSearch(startDate, endDate, wait);

            sleep(1000);

            // Get and store flight information
            storeFlightInformation(wait);



    }

    private void performFlightSearch(String departureDate, String returnDate, WebDriverWait wait) {
        // Locate the new departure and return elements on the new page
        WebElement newDepartureInput = driver.findElement(By.cssSelector(".uNiB1 > div:nth-child(1) > div:nth-child(1) > div:nth-child(1) > div:nth-child(1) > input:nth-child(2)"));
        WebElement newReturnInput = driver.findElement(By.cssSelector(".uNiB1 > div:nth-child(1) > div:nth-child(1) > div:nth-child(2) > div:nth-child(1) > input:nth-child(1)"));

        // Clear existing values
        newDepartureInput.clear();
        newReturnInput.clear();

        // Use JavascriptExecutor to set date values directly
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].value = arguments[1];", newDepartureInput, departureDate);
        js.executeScript("arguments[0].value = arguments[1];", newReturnInput, returnDate);

        // Wait for the new elements to be visible
        wait.until(ExpectedConditions.visibilityOf(newDepartureInput));
        wait.until(ExpectedConditions.visibilityOf(newReturnInput));

        // Add an additional wait for the page to load completely
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".uNiB1"))); // Assuming this element is present on the loaded page
    }

    private void storeFlightInformation(WebDriverWait wait) {
        String airlineName = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"yDmH0d\"]/c-wiz[2]/div/div[2]/c-wiz/div[1]/c-wiz/div[2]/div[2]/div[3]/ul/li[1]/div/div[2]/div/div[2]/div[2]/div[2]"))).getText();
        String price = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("ul.Rk10dc:nth-child(3) > li:nth-child(1) > div:nth-child(1) > div:nth-child(2) > div:nth-child(1) > div:nth-child(2) > div:nth-child(6) > div:nth-child(1) > div:nth-child(2) > span:nth-child(1)"))).getText();

        System.out.println("Airline: " + airlineName);
        System.out.println("Price: " + price);

        // Store the information in the database
        storeFlightInformation(airlineName, price);
    }

    private void storeFlightInformation(String airlineName, String price) {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO flights (airline_name, price) VALUES ('" + airlineName + "', '" + price + "')");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @AfterClass
    public static void cleanUp() {
        // driver.close();
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
