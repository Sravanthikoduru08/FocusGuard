from appium import webdriver
from appium.options.android import UiAutomator2Options
from appium.webdriver.common.appiumby import AppiumBy
import unittest

class FocusGuardAppiumTests(unittest.TestCase):
    def setUp(self):
        # Setup Appium capabilities for FocusGuard Android Application
        options = UiAutomator2Options()
        options.platform_name = 'Android'
        options.device_name = 'emulator-5554'  # Replace with actual device name
        options.app_package = 'com.example.focusguard'
        options.app_activity = '.MainActivity' # Verify main activity name
        options.no_reset = True
        
        # Connect to the local Appium server
        self.driver = webdriver.Remote('http://127.0.0.1:4723', options=options)
        self.driver.implicitly_wait(10)

    def tearDown(self):
        if self.driver:
            self.driver.quit()

    def test_tc_002_app_launch(self):
        """TC_002: Verify app launches successfully"""
        # Wait for an element that signifies successful load, e.g., Onboarding or Dashboard
        dashboard_element = self.driver.find_elements(AppiumBy.ID, 'com.example.focusguard:id/fragment_container')
        self.assertTrue(len(dashboard_element) > 0, "App failed to launch correctly.")
        print("TC_002 Passed: App launched successfully.")

    def test_tc_041_navigate_to_study_mode(self):
        """TC_041: Verify navigation to StudyFragment"""
        # Example of clicking navigation bar to go to Study Fragment
        study_tab = self.driver.find_element(AppiumBy.ID, 'com.example.focusguard:id/nav_study')
        study_tab.click()
        
        # Verify timer is visible
        timer_text = self.driver.find_element(AppiumBy.ID, 'com.example.focusguard:id/timerTextView')
        self.assertIsNotNone(timer_text, "Study timer UI not displayed.")
        print("TC_041 Passed: Study Mode loaded.")

if __name__ == '__main__':
    unittest.main()
