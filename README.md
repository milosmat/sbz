# SBZ (Drools Demo)

I built a knowledge-based (rule-driven) demo using Java 8 and Drools. The backend is a plain Maven Java application packaged as a shaded JAR with an entry point at `primeri.App`. A PostgreSQL schema is included for persistence needs, and a TypeScript front-end directory is present for UI experiments.

- Java: 1.8
- Drools: 7.73.0.Final (kie-api, kie-ci, drools-core, drools-compiler, drools-mvel)
- Database: PostgreSQL (JDBC driver included)
- Logging: slf4j-nop (silences logging for demo)
- Packaging: Maven Shade Plugin (runnable fat JAR)
- JSON: Gson (2.10.1)

## Project structure

```
.
├─ pom.xml
├─ sbnz_schema.sql
├─ src/
│  ├─ main/
│  │  ├─ java/
│  │  │  └─ primeri/App.java            # mainClass configured for the shaded JAR
│  │  └─ resources/                     # rules (DRL), KIE config, application resources
├─ front/                                # TypeScript front-end stub (UI experiments)
├─ deps.txt                               # dependency tree snapshot
└─ bin/                                   # auxiliary build artifacts (contains an older sample POM)
```

Notes:
- The effective build uses `pom.xml` at the repository root with `java.version=1.8` and `drools.version=7.73.0.Final`.
- `bin/pom.xml` is an older snapshot (kept for reference); the root POM is authoritative.

## Building

```bash
mvn clean package
```

This produces a runnable, shaded JAR in `target/` (Shade plugin attaches the shaded artifact). The Shade configuration sets:

- main class: `primeri.App`
- shaded artifact id: `drools-demo-1.0-SNAPSHOT`

Run:

```bash
java -jar target/drools-demo-1.0-SNAPSHOT*.jar
```

(Use the exact filename produced in `target/`.)

## Database

I use PostgreSQL. The driver (`org.postgresql:postgresql:42.7.3`) is declared in the POM.

Initialize schema:

```bash
psql -d <database> -U <user> -h <host> -f sbnz_schema.sql
```

Configure the JDBC URL/credentials in the application as needed (e.g., via config in `src/main/resources` or inline creation of the datasource, depending on the demo variant).

## Rules and runtime

The application loads Drools artifacts via KIE (kie-api/kie-ci) and executes rules over domain facts. Typical flow:

1. Build a KIE container/session from classpath artifacts.
2. Insert facts (domain objects).
3. Fire rules.
4. Consume the results (derived facts, alerts, recommendations).

Rules (DRL files) and related KieModule configuration belong under `src/main/resources` so they are on the classpath for the shaded JAR.

## Front-end (optional)

The `front/` directory holds a TypeScript-based UI (HTML/CSS/SCSS present). It’s separate from the Java build. If I wire it into the demo, I build and serve it independently; otherwise, the Java app demonstrates rule execution via console/API.

## Dependencies (key)

- Drools stack: `org.kie:kie-api`, `org.kie:kie-ci`, `org.drools:drools-core`, `org.drools:drools-compiler`, `org.drools:drools-mvel`
- PostgreSQL JDBC: `org.postgresql:postgresql`
- Gson: `com.google.code.gson:gson`
- Tests: JUnit 4.12
- Shade/Compiler: Maven plugins configured to target Java 8

To regenerate the dependency tree snapshot:

```bash
mvn dependency:tree > deps.txt
```

## Commands summary

```bash
# build
mvn clean package

# run (shaded jar)
java -jar target/drools-demo-1.0-SNAPSHOT*.jar

# init DB (PostgreSQL)
psql -d <db> -U <user> -h <host> -f sbnz_schema.sql
```
