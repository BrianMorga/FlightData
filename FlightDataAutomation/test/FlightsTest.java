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
import org.openqa.selenium.NoSuchElementException;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
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
            connection = DriverManager.getConnection("jdbc:sqlite:newflights.sqlite");
            Statement statement = connection.createStatement();

            // Modify the SQL statement to add a 'destination' column
            String createTableSQL = "CREATE TABLE IF NOT EXISTS flights (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "airline_name TEXT, " +
                    "price TEXT, " +
                    "destination TEXT)";

            System.out.println("Executing SQL Statement: " + createTableSQL);

            statement.executeUpdate(createTableSQL);

            // Commit the changes explicitly
            connection.commit();
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

        for (int i = 0; i < 100; i++) {
            WebElement departureNextButton = driver.findElement(By.cssSelector(".uNiB1 > div:nth-child(1) > div:nth-child(1) > div:nth-child(1) > div:nth-child(1) > div:nth-child(4) > button:nth-child(2)"));
            WebElement returnNextButton = driver.findElement(By.cssSelector(".uNiB1 > div:nth-child(1) > div:nth-child(1) > div:nth-child(2) > div:nth-child(1) > div:nth-child(3) > button:nth-child(2)"));
            departureNextButton.click();
            returnNextButton.click();
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            storeFlightInformation(destination);
        }
    }

    @Test
    @Parameters({"Cancun", "Las Vegas", "Denver", "Rome", "Milan", "Paris", "Madrid", "Amsterdam", "Singapore"})
    public void setupFlightSearch(String destination) throws ParseException, InterruptedException {
        driver.get("https://www.google.com/travel/flights");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        String startDate = "Tue, Apr 30";
        String endDate = "May 7, 2024";

        performInitialSearch(startDate, endDate, destination, wait);

        // Perform search with the given departure and return date
        performFlightSearch(startDate, endDate, wait);

        sleep(1000);

        // Get and store flight information
    }

    private void testWriteOperation() {
        try (Statement statement = connection.createStatement()) {
            // Insert a dummy record for diagnostic purposes
            statement.executeUpdate("INSERT INTO flights (airline_name, price, destination) VALUES ('Test Airline', '100', 'Test Destination')");
            System.out.println("Write operation successful for diagnostic purposes.");
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error during the write operation.");
        }
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

    private void storeFlightInformation(String destination) {
        try {
            WebElement departureDateInput = driver.findElement(By.cssSelector(".uNiB1 > div:nth-child(1) > div:nth-child(1) > div:nth-child(1) > div:nth-child(1) > input:nth-child(2)"));
            String departureDate = departureDateInput.getAttribute("value");
            System.out.println("Departure Date: " + departureDate);

            WebElement airlineElement = driver.findElement(By.xpath("//*[@id=\"yDmH0d\"]/c-wiz[2]/div/div[2]/c-wiz/div[1]/c-wiz/div[2]/div[2]/div[3]/ul/li[1]/div/div[2]/div/div[2]/div[2]/div[2]"));
            String airlineName = airlineElement.getText();
            System.out.println("Airline: " + airlineName);

            // Additional information retrieval and storage
            String price = driver.findElement(By.cssSelector("ul.Rk10dc:nth-child(3) > li:nth-child(1) > div:nth-child(1) > div:nth-child(2) > div:nth-child(1) > div:nth-child(2) > div:nth-child(6) > div:nth-child(1) > div:nth-child(2) > span:nth-child(1)")).getText();

            System.out.println("Price: " + price);
            System.out.println("Destination: " + destination);

            // Store the information in the database
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("INSERT INTO flights (departure_date, airline_name, price, destination) VALUES ('" + departureDate + "', '" + airlineName + "', '" + price + "', '" + destination + "')");
            } catch (SQLException e) {
                e.printStackTrace();
            }

        } catch (NoSuchElementException e) {
            System.out.println("Failed to retrieve departure date or airline name");
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
