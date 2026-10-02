# Security Policy

The Adulting development team and Ferrix Labs take the security and privacy of our users very seriously. As a **100% local-first personal finance application**, Adulting is architected from the ground up to protect financial privacy by minimizing data exposure.

---

## Supported Versions

We provide security updates and patches for the following versions:

| Version | Supported          | Status                               |
| ------- | ------------------ | ------------------------------------ |
| 2.x     | :white_check_mark: | Currently active release             |
| 1.x     | :x:                | Deprecated (please upgrade to 2.x)   |
| < 1.0   | :x:                | Unsupported                          |

---

## Privacy & Security Architecture Principles

1. **Local-First Storage**: All income streams, recurring payments, bills, budget caps, and transactions are stored solely inside a sandboxed SQLite database via Android Room (`adulting_database.db`). No financial information is transmitted to remote servers.
2. **Zero Remote Telemetry**: Adulting contains no advertising SDKs, tracking frameworks, or analytics trackers.
3. **Android Security Features**: The app leverages Android's application sandbox, exact alarm permission model (`SCHEDULE_EXACT_ALARM`), and runtime permissions (`POST_NOTIFICATIONS`) to ensure system-level safety.
4. **Android Keystore & Signing**: Official releases are cryptographically signed using dedicated release keys. Never distribute APKs signed with debug or unknown keys.

---

## Reporting a Vulnerability

If you discover a potential security vulnerability in Adulting, please report it privately. **Do NOT open a public GitHub issue for security vulnerabilities.**

### Submission Process

1. **Email**: Send details of the vulnerability to **security@ferrixlabs.in** or **contact@ferrixlabs.in**.
2. **Content of Report**:
   - Detailed description of the issue.
   - Affected version(s) and device environment (e.g., Android version, device model).
   - Clear step-by-step reproduction steps or proof-of-concept (PoC).
   - Impact assessment: what could an attacker achieve?
3. **Response Timeline**:
   - **Acknowledgment**: Within 48 hours of receipt.
   - **Assessment & Triage**: Within 5 business days.
   - **Remediation & Patch**: Dependent on severity, typically within 14–30 days.
4. **Coordinated Disclosure**:
   - We ask you to follow coordinated vulnerability disclosure. Please allow us reasonable time to fix and release an updated version before any public disclosure.
   - Once resolved, credit will be given in our release notes and changelog unless you wish to remain anonymous.

---

## Security Best Practices for Users & Contributors

- **Device Passcodes & Biometrics**: Because Adulting stores data on-device, keep your device secured with a strong PIN, password, or biometric lock.
- **Sensitive Numbers**: Avoid inputting full credit card numbers, CVVs, or bank account passwords in free-text memo fields.
- **Secrets Management**: If using the optional Gemini AI features, never commit `.env` files or API keys into git. Use `.env.example` as a template.
