#!/bin/bash
# Security testing script for CI/CD pipeline
# This script combines multiple security checks and produces consolidated reports

set -e

echo "Starting security testing for Kotlin Mobile Application"
echo "======================================================="

# Create reports directory
mkdir -p reports/security

# 1. Run OWASP Dependency Check
echo "Running OWASP Dependency Check..."
./gradlew dependencyCheckAnalyze

# 2. Run Detekt Static Analysis with security rules
echo "Running Detekt static analysis with security rules..."
./gradlew detekt

# 3. Check for hardcoded secrets
echo "Checking for hardcoded secrets..."
find . -type f -name "*.kt" -o -name "*.java" -o -name "*.xml" | xargs grep -l "api[_-]key\|secret\|password\|token" > reports/security/potential_secrets.txt || true

# 4. SSL Certificate check
echo "Checking for insecure SSL implementations..."
find . -type f -name "*.kt" -o -name "*.java" | xargs grep -l "X509TrustManager\|TrustAllCerts\|AllowAllHostnameVerifier\|setHostnameVerifier" > reports/security/insecure_ssl.txt || true

# 5. Check for insecure WebView implementations
echo "Checking for insecure WebView implementations..."
find . -type f -name "*.kt" -o -name "*.java" | xargs grep -l "setJavaScriptEnabled(true)\|addJavascriptInterface" > reports/security/webview_issues.txt || true

# 6. Check for exported components
echo "Checking for potentially exported components..."
grep -r "exported=\"true\"" --include="*.xml" . > reports/security/exported_components.txt || true

# 7. Check for insecure file permissions
echo "Checking for insecure file permissions..."
find . -type f -name "*.kt" -o -name "*.java" | xargs grep -l "MODE_WORLD_READABLE\|MODE_WORLD_WRITEABLE\|openFileOutput" > reports/security/file_permission_issues.txt || true

# 8. Check for data leaks in logs
echo "Checking for potential data leaks in logs..."
find . -type f -name "*.kt" -o -name "*.java" | xargs grep -l "Log\.v\|Log\.d\|Log\.i\|Log\.w\|Log\.e" > reports/security/logging_issues.txt || true

# 9. Consolidate report
echo "Generating consolidated security report..."
echo "# Security Testing Report" > reports/security/consolidated_report.md
echo "Generated on: $(date)" >> reports/security/consolidated_report.md
echo "" >> reports/security/consolidated_report.md

echo "## Dependency Check" >> reports/security/consolidated_report.md
echo "See detailed report at: build/reports/dependency-check-report.html" >> reports/security/consolidated_report.md
echo "" >> reports/security/consolidated_report.md

echo "## Static Analysis" >> reports/security/consolidated_report.md
echo "See detailed report at: build/reports/detekt/detekt.html" >> reports/security/consolidated_report.md
echo "" >> reports/security/consolidated_report.md

for report in potential_secrets insecure_ssl webview_issues exported_components file_permission_issues logging_issues; do
  echo "## ${report//_/ }" >> reports/security/consolidated_report.md
  
  if [[ -s "reports/security/${report}.txt" ]]; then
    echo "Issues found:" >> reports/security/consolidated_report.md
    echo '```' >> reports/security/consolidated_report.md
    cat "reports/security/${report}.txt" >> reports/security/consolidated_report.md
    echo '```' >> reports/security/consolidated_report.md
  else
    echo "No issues found." >> reports/security/consolidated_report.md
  fi
  
  echo "" >> reports/security/consolidated_report.md
done

echo "Security testing completed!"
echo "Consolidated report available at: reports/security/consolidated_report.md"

# Exit with error if any serious issues found
if grep -q "Critical" build/reports/dependency-check-report.xml || \
   grep -q "HIGH" build/reports/detekt/detekt.xml || \
   [[ -s "reports/security/insecure_ssl.txt" ]]; then
  echo "SECURITY TESTING FAILED: Critical security issues found!"
  exit 1
fi

exit 0
