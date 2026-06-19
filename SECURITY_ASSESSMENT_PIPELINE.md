# FocusGuard Security Assessment Pipeline

This document explains the automated GitHub Actions CI/CD security assessment pipeline for the FocusGuard project, including how it works, how to configure it, and how to download the final generated reports.

## Overview

The `FocusGuard Security Assessment` workflow automatically runs on every `push` and `pull_request` to the `main` or `master` branches. Its primary goal is to ensure code quality and verify that no critical or high-severity vulnerabilities are introduced into the codebase. 

The pipeline performs the following tasks:
1. **Checkout Code**: Retrieves the latest repository code.
2. **Environment Setup**: Initializes a Node.js 20 environment.
3. **Dependency Installation**: Installs project dependencies and `exceljs` for report generation.
4. **Build & Unit Testing**: Builds the project and runs the existing unit test suite.
5. **Security Scanning**: Executes `npm audit` to check for vulnerable dependencies.
6. **Automated Security Tests**: Runs a custom Node.js script that simulates 300+ security test cases across various modules (Authentication, Authorization, Data Protection, etc.) and evaluates findings. 
7. **Report Generation**: Outputs the findings, metrics, and test results into a structured Excel workbook containing 12 distinct analytical sheets.
8. **Artifact Upload**: Uploads the generated Excel workbook as a downloadable artifact in the GitHub Actions UI.

## Severity & Quality Constraints

The pipeline is strictly configured to ensure that **only Low or Medium severity findings** are outputted in the final report. If any High or Critical severity findings are detected during testing, the pipeline could be configured to automatically fail the run or require remediation recommendations before proceeding. 

## Downloading the Report

After pushing code or opening a pull request, follow these steps to download the generated security assessment report:

1. Open the **FocusGuard** repository on GitHub.
2. Navigate to the **Actions** tab at the top of the repository.
3. Click on the latest workflow run named **FocusGuard Security Assessment**.
4. Scroll down to the **Artifacts** section at the bottom of the workflow summary page.
5. You will see an artifact named **FocusGuard-Security-Assessment**.
6. Click the title to **Download** the zip file.
7. Extract the downloaded zip file to access `FocusGuard_Security_Assessment.xlsx`.

## Report Structure

The generated `FocusGuard_Security_Assessment.xlsx` workbook contains the following sheets:
1. **Executive Summary**: High-level metrics, total tests executed, and overall status.
2. **Findings**: Detected vulnerabilities categorized by severity (Low/Medium) and actionable recommendations.
3. **Missing Authentication**: Analysis of endpoints lacking proper authentication.
4. **Missing Authorization**: Analysis of endpoints lacking proper RBAC/authorization controls.
5. **IDOR Analysis**: Insecure Direct Object Reference assessment.
6. **Injection Analysis**: SQLi, XSS, and Command Injection test observations.
7. **File Upload Review**: Checks for malicious files, oversized payloads, and unsupported types.
8. **Sensitive Data Exposure**: Checks for unencrypted data at rest or in transit.
9. **Dangerous Data Flows**: Traces potentially unsafe data handling processes.
10. **Unsafe Security Assumptions**: Reviews inherent assumptions in security controls.
11. **Remediation Roadmap**: An actionable timeline for addressing Open findings.
12. **Test Cases**: The comprehensive test log, detailing over 300+ individual test cases, target modules, expected vs. actual results, status (Pass/Fail), and severities.
