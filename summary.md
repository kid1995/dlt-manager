# DLT Manager Service

Der DLT Manager Service ist Bestandteil der elpa:4 (vormals ELISA) Microservice-Architektur und fungiert innerhalb der
elpa:4 Service-Familie als zentrale Schaltstelle zur Behandlung von Dead Letter Topic (DLT) Kafka Events.
Er empfängt über einen von mehreren elpa:4 Services gemeinsam genutzten DLT-Topic Informationen über Kafka-Events, die
in einem der Services nicht erfolgreich verarbeitet werden konnten und speichert diese in einer Datenbank.
Die DLT-Message enthält dabei sowohl das Original-Event als auch Metadaten (wie etwa den Ursprung, den Fehlerzeitpunkt
und die Fehlermeldung).

Über die UI des DLT Manager Service können aufgetretene Fehler aufgelistet, gefiltert, angezeigt, inspiziert und behandelt
werden. Abhängig von Art und Ursprung eines DLT-Events können unterschiedliche Admin-Operationen ausgeführt werden, wie
etwa die erneute Zustellung des Original-Events, die Erstellung eines Incident- oder Fehlertickets oder die Löschung
bzw. Archivierung des DLT-Events.
