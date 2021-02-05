# Erprobung und Analyse des Einsatzes von Blockchain-Technologie in der Nahrungsmittel-Lieferkette auf Basis des Systems Corda.

In diesem GitHub Repository liegt der Code der Bachelorarbeit von Johann Heinz.
Ziel der Bachelorarbeit war es eine Lieferkette durch Corda zu modellieren. Für genaueres
zu Corda siehe https://docs.corda.net/docs/corda-os/4.4.html.

# Vorbedingungen

Siehe https://docs.corda.net/docs/corda-os/4.7/getting-set-up.html für die notwendigen Programme und deren Installation.

# Das Netzwerk starten

Die Ausführungen beruhen auf dem CorDapp Template - Kotlin, siehe https://github.com/corda/cordapp-template-kotlin.

## Knoten erzeugen 

Ändern Sie zunächst den Task ``deployNodes`` in der ``build.gradle`` Datei nach Ihren Wünschen ab.
Wurden alle Knoten benannt und haben die richtigen Einstellungen können diese durch den Befehl 

    ``gradlew deployNodes`` 

ausgeführt in einer Kommandozeile im Haupverzeichnis erzeugt werden. 
Die Knoten befinden sich nun im Verzeichnis

    `build/nodes/`.

Jeder Knoten hat dabei einen eigenen Ordner.

## Die Knoten starten

Jeder Knoten kann z.B. über diesen Befehl ausgeführt in einer Kommandozeile im Verzeichnis des Knotens gestartet werden:

    ``java -jar corda.jar``.

Alternativ, um alle gleichzeitig zu Starten, kann im Verzeichnis `build/nodes/` der Befehl ausgeführt werden:

    ``runnodes.jar``

Wurde der Knoten erfolgreich gestartet, zeigt der Knoten folgendes:

    Welcome to the Corda interactive shell.
    You can see the available commands by typing 'help'.

    Fri Feb 05 11:39:01 CET 2021>>>

## Interaktion mit dem Knoten

Der Befehl ``run`` zeigt eine Vielzahl an möglichen Funktionen, die aufgerufen werden können. Mittels dem Befehl 

    ``flow list``

werden alle auf dem Knoten installierten Flows dargestellt und mittels

    ``flow start Flowname Flowparameter``

kann ein Flow gestartet werden, im Beispiel der ProduceFlow:

    ``flow start ProduceFlow weight: 25, number: 1, desc: "Biologisch", values: " "``

Dieser Flow erzeugt einen Traubenkisten Datensatz mit 25 kg Gewicht und der Beschreibung Biologisch.

## Der WebServer

Die jar Datei des WebServers wird über den Befehl gestartet im Hauptverzeichnis in einer Kommandozeile erstellt:

    ``gradlew clients:bootjar``.

Die Datei liegt dann im Verzeichnis `clients/build/libs/` und kann z.B. gestartet werden durch:

    ``java -jar corda-webserver.jar --spring.config.location=Pfad/zur/Datei/application.properties``.

In der application.properties Datei sind wichtige Informationen hinterlegt, sie sieht beispielsweise so aus:
    
    server.port=10055
    config.rpc.username=user1
    config.rpc.password=test
    config.rpc.host=localhost
    config.rpc.port=10006

Die Parameter username und password sind standardmäßig so eingestellt, rpc.port beschreibt den RPC Port des
entsprechenden Knotens und muss mit diesem übereinstimmen. Die Einstellungen server.port und rpc.host beschreiben unter welcher Web-Adresse der WebServer aufgerufen werden kann. Im Beispiel unter:

    ``localhost:10055``

Über den WebServer kann nun mit dem Knoten kommuniziert werden.

# Eigene Implementierungen ergänzen

## Ändern der Contracts, States und Flows

Die Implementierung der Contracts und States finden Sie im Unterverzeichnis `contracts/src/main/kotlin/com/template/`,
die der Flows unter `workflows/src/main/kotlin/com/template/flows`. 

Um neue CorDapps zu erzeugen, müssen Sie neue jar Dateien erzeugen, dazu im Hauptverzeichnis in einer Kommandozeile:

    ``gradlew jar``

ausführen.

Die neuen jar Dateien befinden sich in den Verzeichnissen

        `contracts/build/libs`

    bzw.
    
        `workflows/build/libs`.

Um die CorDapps auf den Knoten zu installieren müssen beide jar Dateien in das Verzeichnis `cordapps` jedes Knotens kopiert werden.

## Der WebServer

Der WebServer kann im Verzeichnis `clients/src/main` angepasst werden, die API Endpunkte befinden sich im Verzeichnis `clients/src/main/kotlin/com/template/webserver/Controller.kt`, die Webseite unter `clients/src/main/resources/static/`
