import os
import re

def scan_android_manifest():
    manifest_path = "app/src/main/AndroidManifest.xml"
    issues = []
    if not os.path.exists(manifest_path):
        return [f"Manifest not found at {manifest_path}"]
        
    with open(manifest_path, 'r', encoding='utf-8') as f:
        content = f.read()
        
    # Check allowBackup
    if 'android:allowBackup="true"' in content:
        issues.append("[MEDIUM] android:allowBackup is set to true. This allows adb backup extraction of sensitive local app data.")
        
    # Check cleartextTraffic
    if 'android:usesCleartextTraffic="true"' in content:
        issues.append("[HIGH] android:usesCleartextTraffic is set to true. Cleartext HTTP traffic is permitted, exposing the app to MITM attacks.")
        
    # Check accessibility service configuration
    if 'service.CognitiveEyesService' in content and 'android:exported="false"' in content:
        issues.append("[MEDIUM] CognitiveEyesService Accessibility Service is marked exported='false'. The OS might fail to bind to it.")

    return issues

def scan_source_code_for_secrets():
    issues = []
    # Look for files in app/src/main
    key_patterns = [
        r'(?i)api[-_]?key\s*=\s*["\'][a-zA-Z0-9_\-]{10,}["\']',
        r'(?i)client[-_]?secret\s*=\s*["\'][a-zA-Z0-9_\-]{10,}["\']',
        r'AIzaSy[a-zA-Z0-9_\-]{33}' # Google/Gemini key pattern
    ]
    
    src_dir = "app/src/main"
    if not os.path.exists(src_dir):
        return issues
        
    for root, dirs, files in os.walk(src_dir):
        for file in files:
            if file.endswith(('.kt', '.java', '.xml', '.properties')):
                file_path = os.path.join(root, file)
                try:
                    with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                        lines = f.readlines()
                    for line_num, line in enumerate(lines, start=1):
                        for pattern in key_patterns:
                            if re.search(pattern, line):
                                issues.append(f"[HIGH] Potential hardcoded secret or API key found in {file_path}:{line_num}")
                except Exception as e:
                    pass
    return issues

def run_scan():
    print("=" * 60)
    print("FOCUSGUARD STATIC SECURITY SCANNER")
    print("=" * 60)
    
    manifest_issues = scan_android_manifest()
    source_issues = scan_source_code_for_secrets()
    
    all_issues = manifest_issues + source_issues
    
    print(f"Scanned app/src/main/AndroidManifest.xml and source directories.")
    print(f"Found {len(all_issues)} potential security vulnerabilities/issues:\n")
    
    if all_issues:
        for issue in all_issues:
            print(issue)
    else:
        print("[INFO] No high or medium risk security issues identified in static analysis.")
        
    print("=" * 60)
    
    # Write to a report file
    with open("android-security-violations.txt", "w", encoding="utf-8") as f:
        f.write("FocusGuard Android App Static Security Scan Report\n")
        f.write("=" * 50 + "\n\n")
        if all_issues:
            for issue in all_issues:
                f.write(issue + "\n")
        else:
            f.write("No high or medium risk security issues identified in static analysis.\n")
            
    print("Saved static scan report to android-security-violations.txt")

if __name__ == "__main__":
    run_scan()
