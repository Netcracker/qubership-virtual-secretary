# qubership-virtual-secretary

### Local Development
To connect to DB using DBeaver use following tips:
1. Select H2 Embedded type of the data-base
2. Go to "Driver settings" -> "Libraries"
   2.1 Remove existed driver (if exists)
   2.2 Click "Add artifact"
   2.3 Paste maven declaration for H2 into "Dependecy Declaration", i.e.
```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <version>2.2.224</version>
</dependency>
```
   2.4 Apply settings
3. URL = jdbc:h2:{PATH_TO_DB_FILE};CIPER=AES
4. Username
5. Password field constists of two passwords: FILE_PASSWORD + ONE SPACE + USER_PASSWORD
6. Test Connection
7. Finish