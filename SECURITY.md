# Security Policy - IGNORE FOR NOW, CLAUDE WENT AWOL

## Supported Versions

| Version | Supported          | Security Status |
| ------- | ------------------ | --------------- |
| 1.0.x   | :white_check_mark: | Best Effort Support   |
| 0.9.x   | :x:                | Probably broken |

## Important

No support or guarantees for function, safety or security of any sorts.
Expect that this software will kill your dog and eat it. There will be bugs. It will likely not be fit for the purpose you intend to use it for. You might loose data, passwords or encounter security incidents.

It is explicitly forbidden to use it for any purpose that would be, direct or indirectly, be connected to anything that would be related to safety or security of building, entity, machinery, human life, etc. You have been warned; use at your own risk.

## Reporting a Vulnerability

If you discover a security vulnerability in SieveEditor, please report it by:

1. **DO NOT** open a public GitHub issue
2. Or use GitHub Security Advisories (private disclosure)

Please include:

- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if available)

We aim to respond within 48 hours and release a fix within 7 days for critical issues.

## Security Best Practices

### For Users

1. **Keep Updated:** Always use the latest version
2. **Verify Downloads:** Check SHA256 checksums of downloaded packages
3. **Use Valid Certificates:** Don't use self-signed certificates for production servers
4. **Strong Server Passwords:** Use strong, unique passwords for ManageSieve accounts

### For Developers

1. **Never Commit Secrets:** Use `.gitignore` for sensitive files
2. **Code Review:** All security-related changes require review
3. **Dependency Scanning:** Run `mvn dependency:tree` and check for vulnerabilities
4. **Static Analysis:** CodeQL scans run automatically on PRs

## Acknowledgments

Security vulnerabilities were identified through:

- GitHub CodeQL Advanced Security scanning
- Manual security code review
- Community contributions

Thank you to all security researchers and contributors who help keep SieveEditor secure.
