const { Builder, By, until } = require('selenium-webdriver');
const chrome = require('selenium-webdriver/chrome');
const assert = require('assert');

describe('FocusGuard Web E2E Onboarding Test', function () {
  this.timeout(30000);
  let driver;

  before(async function () {
    const options = new chrome.Options();
    // Run headless in GitHub Actions environment
    if (process.env.CI) {
      options.addArguments('--headless');
      options.addArguments('--no-sandbox');
      options.addArguments('--disable-dev-shm-usage');
      options.addArguments('--window-size=1920,1080');
    }
    
    driver = await new Builder()
      .forBrowser('chrome')
      .setChromeOptions(options)
      .build();
  });

  after(async function () {
    if (driver) {
      await driver.quit();
    }
  });

  it('should navigate to the onboarding page and establish connection', async function () {
    // Navigate to the local server containing our stable IDs
    await driver.get('http://localhost:5173');

    // Wait for the quote-input to be visible on the page
    const quoteInput = await driver.wait(
      until.elementLocated(By.id('quote-input')),
      15000,
      'Quote input not found'
    );
    
    // Type in a motivational quote
    await quoteInput.clear();
    await quoteInput.sendKeys('I build for my family\'s better tomorrow.');

    // Find the establish button and click it to submit onboarding using JS click
    const establishButton = await driver.findElement(By.id('establish-button'));
    await driver.executeScript('arguments[0].click();', establishButton);

    // Verify it successfully navigated to the main dashboard
    try {
      const dashboardContainer = await driver.wait(
        until.elementLocated(By.id('dashboard-container')),
        10000,
        'Dashboard container not found'
      );
      const isDisplayed = await dashboardContainer.isDisplayed();
      assert.strictEqual(isDisplayed, true, 'Dashboard should be displayed after onboarding');
      console.log('E2E Onboarding Test Passed successfully!');
    } catch (err) {
      // Capture screenshot on failure to debug
      const image = await driver.takeScreenshot();
      require('fs').writeFileSync('selenium-failure.png', image, 'base64');
      console.log('Saved screenshot of failure to selenium-failure.png');

      // Fetch and print browser console logs
      try {
        const logs = await driver.manage().logs().get('browser');
        console.log('=== BROWSER CONSOLE LOGS ===');
        logs.forEach((log) => {
          console.log(`[${log.level.name}] ${log.message}`);
        });
        console.log('============================');
      } catch (logErr) {
        console.log('Could not retrieve browser logs:', logErr.message);
      }

      throw err;
    }
  });
});
