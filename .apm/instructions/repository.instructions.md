---
description: Project-specific development instructions for Qubership Virtual Secretary.
applyTo: "**"
---

## Project summary

- Java 24 + Maven project for "Qubership Virtual Secretary".
- Integrations: Mattermost notifications, Google Sheets/Forms for weekly reports, and OpenAI-compatible GenAI for
  report analysis.
- Local state stored in an embedded H2 database file under `data/`.

## Key directories

- `src/main/java`: application code.
- `src/main/resources`: runtime resources (properties, etc.).
- `src/test`: tests (JUnit 4).
- `data/`: local DB files.
- `.qubership`: contains specific configuration for an external scanning tool. Ignore this directory in development
  and refactoring cycles.

## Build

- Requires JDK 24 (see `pom.xml`). If needed, it can be downgraded to JDK 21, but this has not been verified.
- Build the fat JAR:
  - `mvn -q -DskipTests package`
  - Output: `target/vsec.jar` (assembled with dependencies).

## Run locally

- Ensure `app.properties` exists (see below).
- Run the assembled JAR:
  - `java -jar target/vsec.jar`

## Configuration

- `app.properties` expects keys for Mattermost host/token, Google Sheets IDs, OpenAI URL/token, DB credentials, and
  other settings documented in `README.md`.
- The team member list is provided through the `ALL_QS_MEMBERS` secret. Its contents are mounted to a temporary file
  referenced by `QUBERSHIP_TEAM_CONFIG_FILE`.

## Database

- The H2 Embedded DB file is stored under `data/`.
- DBeaver setup steps are in `README.md`.
- Schema (from `com.netcracker.qubership.vsec.db`):
  - `my_db_map` (persistent key/value store for application state):
    - `key_name` VARCHAR(256) PRIMARY KEY
    - `key_value` VARCHAR(4096)
  - `my_db_sheet` (user data downloaded from Google Sheets and post-processed using AI):
    - `id` INT AUTO_INCREMENT PRIMARY KEY
    - `created_when` VARCHAR(50) NOT NULL
    - `reporter_email` VARCHAR(255) NOT NULL
    - `reporter_name` VARCHAR(100)
    - `report_date` VARCHAR(50) NOT NULL
    - `msg_done` VARCHAR
    - `msg_plans` VARCHAR
    - `genai_content_score` INT DEFAULT 0
    - `genai_impact_score` INT DEFAULT 0
    - `genai_proactivity_score` INT DEFAULT 0
    - `genai_context_score` INT DEFAULT 0
    - `genai_final_score` INT DEFAULT 0
    - `genai_analysis_content` VARCHAR
    - `genai_analysis_impact` VARCHAR
    - `genai_analysis_proactivity` VARCHAR
    - `genai_analysis_context` VARCHAR
    - `genai_analysis_strength` VARCHAR
    - `genai_analysis_improvements` VARCHAR
  - Rows are inserted from Google Sheets (`SheetRow`) and then updated with GenAI analysis.
  - Cleanup logic keeps only the latest report per `(reporter_email, reporter_date)` based on `created_when`.

## External services

- Mattermost (bot token).
- Google Sheets API and Google Forms source data.
- OpenAI-compatible API endpoint.

## CI and automation

- The GitHub Actions workflow references the `ALL_QS_MEMBERS` secret and wires `app.properties`; see
  `.github/workflows/main.yml`.

## Conventions

- Main entry point: `com.netcracker.qubership.vsec.VirtualSecretaryApp` (configured in `pom.xml`).
- Logs use SLF4J and Log4j2.
