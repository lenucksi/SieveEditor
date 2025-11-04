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

This is a Maven multi-module project. Simply run:

```bash
./build.sh
```

Or use Maven directly:

```bash
mvn clean package -Dmaven.javadoc.skip=true -DskipTests
```

The build will:
1. Compile ManageSieveJ (lib/ManageSieveJ/)
2. Compile SieveEditor (app/)
3. Create the standalone JAR

### Run

```bash
./sieveeditor.sh
```

Or:

```bash
java -jar app/target/SieveEditor-jar-with-dependencies.jar
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

