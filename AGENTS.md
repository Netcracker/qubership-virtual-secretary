# AGENTS.md

## Project summary
- Java 24 + Maven project for "Qubership Virtual Secretary".
- Integrations: Mattermost notifications, Google Sheets/Forms for weekly reports, DeepSeek (OpenAI-compatible) for report analysis.
- Local state stored in an embedded H2 database file under `data/`.

## Key directories
- `src/main/java`: application code.
- `src/main/resources`: runtime resources (properties, etc.).
- `src/test`: tests (JUnit 4).
- `data/`: local DB files.
- `.qubership`: contains specific configuration for external scanning tool. Ignore this directory in development and refactoring cycles. 

## Build
- Requires JDK 24 (see `pom.xml`). If needed - can be downgraded to JDK 21, but not verified yet.
- Build fat JAR:
  - `mvn -q -DskipTests package`
  - Output: `target/vsec.jar` (assembled with dependencies).

## Run (local)
- Ensure `app.properties` exists (see below).
- Run the assembled JAR:
  - `java -jar target/vsec.jar`

## Configuration
- `app.properties` expected keys (see `README.md`): Mattermost host/token, Google Sheets IDs, DeepSeek URL/token, DB credentials, etc.
- Team member list is provided via `ALL_QS_MEMBERS` secret; its contents are mounted to a temp file and referenced by `QUBERSHIP_TEAM_CONFIG_FILE`.

## Database
- H2 Embedded DB file is stored under `data/`.
- DBeaver setup steps are in `README.md`.
- Schema (from `com.netcracker.qubership.vsec.db`):
  - `my_db_map` (persistent key/value store to keep useful states for different application flags.)
    - `key_name` VARCHAR(256) PRIMARY KEY
    - `key_value` VARCHAR(4096)
  - `my_db_sheet` (Contains users data downloaded from Google sheets and post-processed using AI)
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
  - Notes:
    - Rows are inserted from Google Sheets (`SheetRow`) and then updated with GenAI analysis.
    - Cleanup logic keeps only the latest report per `(reporter_email, report_date)` based on `created_when`.

## External services
- Mattermost (bot token).
- Google Sheets API + Google Forms source data.
- DeepSeek (OpenAI-compatible API endpoint).

## CI/automation
- GitHub Actions workflow mentions `ALL_QS_MEMBERS` secret and `app.properties` wiring (see `.github/workflows/main.yml`).

## Conventions
- Main entrypoint: `com.netcracker.qubership.vsec.VirtualSecretaryApp` (configured in `pom.xml`).
- Logs via SLF4J + Log4j2.
