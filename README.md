# java-imaging-test

Test cases for problems with Java imaging APIs.

Run e.g. the `TestProfileLoading` tests as

```
mvn exec:java -Dexec.mainClass=TestProfileLoading
```

or with shorter logging messages

```
mvn exec:java -Dexec.mainClass=TestProfileLoading -Djava.util.logging.SimpleFormatter.format='%5$s%6$s%n'
```

or with a more verbose log level
```
mvn exec:java -Dexec.mainClass=TestProfileLoading -Djava.util.logging.SimpleFormatter.format='%5$s%6$s%n' -Djava.util.logging.ConsoleHandler.level=FINE
```
