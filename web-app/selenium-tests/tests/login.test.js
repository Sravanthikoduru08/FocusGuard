const { Builder, By, until } = require('selenium-webdriver');
const chrome = require('selenium-webdriver/chrome');
const firefox = require('selenium-webdriver/firefox');
const edge = require('selenium-webdriver/edge');
const assert = require('assert');
const fs = require('fs');

describe('FocusGuard Web E2E Onboarding Test', function () {
  this.timeout(45000);
  let driver;
  const browserName = (process.env.BROWSER || 'chrome').toLowerCase();

  before(async function () {
    console.log(`Initializing Selenium WebDriver for browser: ${browserName}`);
    let builder = new Builder().forBrowser(browserName);

    if (browserName === 'chrome') {
      const options = new chrome.Options();
      if (process.env.CI) {
        options.addArguments('--headless');
        options.addArguments('--no-sandbox');
        options.addArguments('--disable-dev-shm-usage');
        options.addArguments('--window-size=1920,1080');
      }
      builder = builder.setChromeOptions(options);
    } else if (browserName === 'firefox') {
      const options = new firefox.Options();
      if (process.env.CI) {
        options.addArguments('--headless');
      }
      builder = builder.setFirefoxOptions(options);
    } else if (browserName === 'edge') {
      const options = new edge.Options();
      if (process.env.CI) {
        options.addArguments('--headless');
        options.addArguments('--no-sandbox');
        options.addArguments('--disable-dev-shm-usage');
        options.addArguments('--window-size=1920,1080');
      }
      builder = builder.setEdgeOptions(options);
    } else {
      throw new Error(`Unsupported browser: ${browserName}`);
    }

    driver = await builder.build();
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
      20000,
      'Quote input not found'
    );
    
    // Wait for hydration to complete (async scripts to run and bind events)
    await driver.sleep(5000);
    
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
        15000,
        'Dashboard container not found'
      );
      const isDisplayed = await dashboardContainer.isDisplayed();
      assert.strictEqual(isDisplayed, true, 'Dashboard should be displayed after onboarding');
      console.log(`E2E Onboarding Test Passed successfully on ${browserName}!`);

      // Capture success screenshot to verify it renders correctly
      const image = await driver.takeScreenshot();
      const screenshotName = `selenium-success-${browserName}.png`;
      fs.writeFileSync(screenshotName, image, 'base64');
      console.log(`Saved screenshot of success to ${screenshotName}`);
    } catch (err) {
      // Capture screenshot on failure to debug
      const image = await driver.takeScreenshot();
      const screenshotName = `selenium-failure-${browserName}.png`;
      fs.writeFileSync(screenshotName, image, 'base64');
      console.log(`Saved screenshot of failure to ${screenshotName}`);

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

      // Fetch and print DOM page source
      try {
        const html = await driver.getPageSource();
        console.log('=== PAGE SOURCE ON FAILURE ===');
        console.log(html);
        console.log('==============================');
      } catch (htmlErr) {
        console.log('Could not retrieve page source:', htmlErr.message);
      }

      throw err;
    }
  });
});
