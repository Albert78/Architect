# Architect
2D/3D CAD Raum- und Wohnungsplaner

![2D view](Documentation/Screenshots/Construction-Create-Wall-2D.png)
![2D view](Documentation/Screenshots/Furniture-Living-Room-2D.png)
![3D view](Documentation/Screenshots/Selection-Living-Room-3D.png)
![Material Library](Documentation/Screenshots/Material-Library.png)
![Material Editor](Documentation/Screenshots/Material-Editor.png)
![SupportObject Library](Documentation/Screenshots/SupportObject-Library.png)
![SupportObject Editor](Documentation/Screenshots/SupportObject-Editor.png)

## tl;dr
Notwendig:
- JDK Version >= 21

```
cd Architect
gradlew :main:run
```

## Was ist Architect?
Architect ist ein **Grundriss-Planer** mit **3D-Ansichtsfunktion** als komplett quelloffenes Java-Programm. Wer es zum Laufen bekommt, sollte mit der Bedienung keine Probleme haben. Etwas Technik-Affinität schadet nicht. Das Programm eignet sich gut, um eine Wohnung zu zeichnen, zu möblieren, Möbel/Wände/Oberflächen zu ändern und sich das Resultat in 2D und 3D darstellen zu lassen. Die Funktionalität ist vergleichbar mit SweetHome3D, es hat zwar noch weniger Funktionen, ist dafür aber moderner und kann ein paar Dinge besser, z.B. Dachschrägen, bessere 3D-Darstellung und Usability, ... Außerdem können alle Möbel von SweetHome3D importiert werden, solange der Möbel-Designer noch so rudimentär ist und wir noch keine eigenen öffentlichen Möbel-Bibliotheken haben.
Die Oberfläche liegt aktuell nur in deutscher Sprache vor.
Funktional liegt der Fokus auf **Wohnungsplanung** und **Innenarchitektur**, also der Visualisierung einer Wohnung.
Als Anwendungsentwickler ist mir neben guter Usability vor allem die Codearchitektur sehr wichtig. Hier liegt mein Fokus auf durchdachter Softwarearchitektur, sauberer Programmierung, guter Wartbarkeit, einfacher Erweiterbarkeit aber auch durchdachter Benutzerführung und Anwendbarkeit.
Das Programm wird immer mal wieder erweitert und verbessert, ich nehme auch gerne sinnvolle Featurewünsche oder andere Vorschläge entgegen.

## Was ist Architect nicht?
Erwartet bitte (noch) kein komplett fertiges Programm. Es kann im aktuellen Zustand über IntelliJ oder Gradle gestartet werden, über Gradle kann auch eine installierbare Version gebaut werden.
Das Programm ist aktuell ausschließlich in deutsch lokalisiert, andere Sprachen können programmatisch leicht ergänzt werden.

Außerdem:
- Architect ist keine BIM-Software, d.h. es können keine Baumaterialien modelliert werden (das ist auch nicht geplant)
- Architect ist ist nicht auf Kompatibilität zu anderen CAD-Programmen ausgelegt, d.h. es können (noch) keine Dateien aus anderen Formaten (Autodesk-Programme etc.) importiert oder in andere Formate exportiert werden
- Aufgrund der verwendeten 3D-Bibliothek (JavaFX) ist die Darstellung von indirektem Licht und Schattenwurf derzeit nicht oder nur sehr eingeschränkt möglich.

## Funktionen
- Wände, Türen und Fenster (aktuell nur Wand-Löcher, wird noch ausgebaut), Böden, Decken, Dachschrägen modellieren
- Möblierung und Objekte (Oberflächen-Begriff: Hilfsobjekte)
- Konfiguration ("Anmalen") von Oberflächen (Wände, Böden, Möbel, ...)
- Lineal und Hilfslinien
- 3D-Darstellung und intuitive, virtuelle Navigation durch das Objekt mit den Maustasten und Scrollrad
- Möbel- und Oberflächenbibliotheksverwaltung
	- Eigenes Bibliotheksformat, Möbel/Hilfsobjekte werden im .obj-Format, Oberflächen im .mtl-Format mit zusätzlichen Metadaten verwaltet
	- Einfacher Editor zum Anlegen und Ändern von Möbeln und Oberflächen
	- Bibliotheksimporter für Möbel- und Oberflächenbibliotheken aus SweetHome3D

## Was kommt noch?
- Erweiterung des Plan-Modells, so dass in einer Datei / einem Workspace mehrere Pläne enthalten sind, die relativ zueinander platziert sind. Also z.B. pro Etage oder sogar pro Zimmer einen eigenen Plan, die in der 3D-Ansicht zusammen dargstellt werden.
Das Dateihandling ist noch nicht ganz fertig; Architect sieht das Verzeichnis einer Plan-Datei als Workspace, es kommen später weitere Dateien neben der Plan-XML-Datei hinzu wie z.B. lokale Hilfsobjekte und Materials und ggf. weitere Plan-Dateien. Im Moment muss man noch im Öffnen-Dialog die Plan-XML-Datei explizit anklicken zum Öffnen.
- Bessere Konfigurationsmöglichkeiten für Oberflächen auf Objekten, z.B. Lage, Skalierung und Drehung (z.B. für Dachziegeln, Tapeten- und Bodentexturen etc.)
- Türen und Fenster in Wandöffnungen, Modellierung von verschiedenen Etagen und Übergänge/Treppen
- Lampen, die auch leuchten :-)
- Weitere Arten von Hilfsobjekten, z.B. Balken, Treppen etc.
- ...

## Programmiersprache, Ausführung, Installation
#### Programmiersprache und Build-System
- Das Programm ist in Java geschrieben, komplett modularisiert, die grafische Oberfläche verwendet JavaFX, das alles in Java 21 (Stand Februar 2024)
- Das System besteht aus einer Handvoll Modulen, die mithilfe von Gradle gebaut werden
- Das Programm, die Möbel- und Oberflächenbibliotheksverwaltung und der SweetHome3D-Bibliotheksimporter können über Gradle bzw. aus einer IDE (z.B. IntelliJ) gestartet werden

### Quick-Start mit Gradle
#### Benötigt:
- JDK Version >= 21
- Download des Architect-Repositories über `git clone`

#### Bauen und Ausführen über Gradle:
- Architect-Hauptprogramm: `gradlew :main:run`
- SweetHome3D-Importer: `gradlew :sh3dimporter:run`

### Quick-Start mit IntelliJ
#### Benötigt:
- IntelliJ
- JDK Version >= 21
- Download des Architect-Repositories über `git clone`

#### Ausführen über IntelliJ:
- Ausführen von `de.dh.cad.architect.ArchitectApplication` im Modul `main`

### Verwendung des Programms
Zu Beginn solltet ihr euch mit der Architect Hauptanwendung vertraut machen, sie läuft ohne weitere Vorbereitung. Es gibt einen 2D- und einen 3D-Modus. Ein Plan wird im 2D-Modus erstellt und kann jederzeit im 3D-Modus angesehen werden. Macht euch mit der Mausbedienung vertraut, erstellt Wände, Böden, Decken, schaut euch das Resultat in 3D an und macht euch mit dem Konzept der Verbindungsanker vertraut.

Zur Gestaltung von Objektoberflächen (Wand-Texturen, Möbelfarben, ...) und zur Möblierung braucht ihr mindestens eine *Asset-Bibliothek*, die ihr mit dem Bibliotheksmanager anlegt. Dieser wird über das Architect-Menü oder als eigenständige Anwendung gestartet. Darin könnt ihr die einzelnen Oberflächen und Möbelstücke von Hand anlegen oder über den SweetHome3D-Importer aus dessen Bibliotheken importieren. Ihr braucht die entpackten Bibliotheken, die dort im git im Projekt "3DModels" eingecheckt sind (Siehe https://svn.code.sf.net/p/sweethome3d/code/trunk).

Importiert Das Hauptverzeichnis in IntelliJ und führt die Main-Klassen aus:
- Aus dem main-Modul/Projekt:
  - **Hauptanwendung:** `de.dh.cad.architect.ArchitectApplication`
  - **Bibliotheksverwaltung:** `de.dh.cad.architect.AssetManagerApplication`
- Aus dem Modul sh3dimporter:
  - **SweetHome3D-Bibliotheksimporter:** `de.dh.cad.architect.libraryimporter.TextureImporterMain` und `de.dh.cad.architect.libraryimporter.SupportObjectsImporterMain`

## Motivation hinter Architect
Das Programm ist aus zwei Bedürfnissen entstanden:
1. Ich wollte eine kostenlose Visualisierungssoftware für meine neue Wohnung, mit der ich Änderungen (Wände einreißen, streichen, ... Möblierung, ...) planen kann ohne die Einschränkungen und Unzulänglichkeiten der am Markt verfügbaren Software
2. Ich brauchte eine nicht-triviale Java-Anwendung, mit der ich meinen Studenten die Kunst des Programmierens beibringen kann, klein genug, um sie vollständig überblicken zu können und groß genug, um daran Verfahren der Softwareentwicklung zu üben und Probleme zu diskutieren

Nach ein paar Gehversuchen mit der exzellenten JavaFX-Bibliothek wuchs bei mir schnell der Wunsch, auf dieser Basis eine größere Software zu bauen. So begann ich vor ein paar Jahren mit der Entwicklung von Architect. Seitdem entwickle ich das Programm immer mal wieder weiter.
