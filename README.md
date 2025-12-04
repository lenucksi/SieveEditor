# SieveEditor

[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Flenucksi%2FSieveEditor.svg?type=shield)](https://app.fossa.com/projects/git%2Bgithub.com%2Flenucksi%2FSieveEditor?ref=badge_shield)

A desktop editor for Sieve mail filter scripts on ManageSieve-compatible servers.

## Screenshots

<details>
<summary>View Screenshots</summary>

### Main Window

![Main Window](screenshots/mainwindow.png)

### Script Management

![Manage Scripts](screenshots/manage-scripts.png)

### Connection Dialog

![Connect](screenshots/connect.png)

### Search and Replace

![Search Replace](screenshots/searchreplace.png)

### Sieve Menu

![Sieve Menu](screenshots/menu-sieve.png)

### Template Menu

![Template Menu](screenshots/menu-template.png)

### Local Operations Menu

![Local Operations](screenshots/menu-localops.png)

### Script Validation

![Script Check](screenshots/script-check.png)

</details>

## Building from Source

### Prerequisites

- Java 21 LTS or later
- Maven 3.6+
- Git

### Clone

```bash
git clone https://github.com/lenucksi/SieveEditor.git
cd SieveEditor
```

### Build

Build using Maven:

```bash
mvn clean package
```

This will create `target/SieveEditor-jar-with-dependencies.jar`

### Run

```bash
java -jar target/SieveEditor-jar-with-dependencies.jar
```

## Features

- Multiple account profiles support
- Syntax highlighting for Sieve scripts
- Find/Replace functionality
- HiDPI/4K display support
- Script validation
- Direct server connection via ManageSieve protocol
- **Local file editing** - Open/save .sieve files locally (Ctrl+L / Ctrl+Shift+S)
- **Template insertion** - Built-in templates for common Sieve patterns

## Documentation

- [CONTRIBUTING.md](CONTRIBUTING.md) - Contributing guidelines and development workflow
- [README-TESTS.md](README-TESTS.md) - Test infrastructure and coverage
- [SECURITY.md](SECURITY.md) - Security policy and vulnerability reporting

## Important

No support or guarantees for function, safety or security of any sorts.
Expect that this software will kill your dog and eat it. There will be bugs. It will likely not be fit for the purpose you intend to use it for. You might loose data, passwords or encounter security incidents.

It is explicitly forbidden to use it for any purpose that would be, direct or indirectly, be connected to anything that would be related to safety or security of building, entity, machinery, human life, etc. You have been warned; use at your own risk.

## License

[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Flenucksi%2FSieveEditor.svg?type=large)](https://app.fossa.com/projects/git%2Bgithub.com%2Flenucksi%2FSieveEditor?ref=badge_large)
