# SieveEditor

A Editor for Sieve Scripts on a compatible ManagedSieve-Server

## Building from Source

### Prerequisites
- Java 21 LTS or later
- Maven 3.6+
- Git

### Clone with Submodules

This project includes ManageSieveJ as a git submodule. Clone with:

```bash
git clone --recurse-submodules https://github.com/Zwixx/SieveEditor.git
```

Or if already cloned:

```bash
git submodule update --init --recursive
```

### Updating Submodules

To update ManageSieveJ to the latest version from the fork:

```bash
./update-submodules.sh
```

Or manually:

```bash
cd lib/ManageSieveJ
git pull origin master
cd ../..
git add lib/ManageSieveJ
git commit -m "Update ManageSieveJ submodule"
./build.sh
```

### Build

Use the build script (recommended):

```bash
./build.sh
```

Or manually:

```bash
# Build ManageSieveJ dependency
mvn -f lib/ManageSieveJ/pom.xml clean install -DskipTests -Dmaven.javadoc.skip=true

# Build SieveEditor
mvn clean package
```

### Run

```bash
./sieveeditor.sh
```

Or:

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

## Documentation

See [dev-docs/](dev-docs/) for detailed documentation:
- [IMPLEMENTATION-STATUS.md](dev-docs/IMPLEMENTATION-STATUS.md) - Project status and history
- [MANAGESIEVEJ-FORK-ANALYSIS.md](dev-docs/MANAGESIEVEJ-FORK-ANALYSIS.md) - Dependency analysis

