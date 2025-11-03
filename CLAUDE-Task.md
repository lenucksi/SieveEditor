Probleme wie folgt:

- App speichert credentials im Home, als JSON vermutlich. Dort kann immer nur ein Account angelegt werden, daher muss ich mit Symlinks arbeiten um mehrere accounts mit dem tool editieren zu können.
- Es soll wohl eine suchen & ersetzen funktion im editor sein, aber die funktioniert nicht.
- Aktuell wird unter Linux leider die UI auf einem 4K Monitor winzig gerendert, das war vorher nicht der Fall, da skalierte es normal mit 4k. Das ist vermutlich ein Bug in entweder einer aktualisierten Version von Gnome mit der Java nicht zurecht kommt, oder in der App, oder in Java. Evtl. wird man einen Shell Wrapper mit passenden Env-Variablen bauen müssen? Finde heraus warum das der Fall ist, dokumen tiere hypothesen und wege sie zu prüfen, zeige mir den report in einer datei.
- beim editieren kann teilweise der letzte buchstabe auf der zeile nicht mehr erreicht werden was zu seltsamen fehlern in bestimmten zielen führt.


- vermutlich sind etliche dependencies uralt, gammlig, abandoned oder was weiß ich. aber dies ist er der einzige editor der tatsächlich funktioniert...

Nice to have wäre:
- Die Skripte von der Platte lokal laden und speichern zu können
- Templating um bestimmte sich wiederholende Elemente einfach mit neuen parametern im skript einbauen zu können
- mehrere accounts in der ui auswählen  können zu denen sich verbunden wird.
- Script was alles mit dependencies baut. 
- Flatpak für alles. Und ein Ding was ein DMG draus baut, aber das nur mit mindestprio. Vielleicht gibt's für beides ja ein MAvenplugin?


Wichtig:
- Das ist eine Mini-App. Sie ist kruschtig und nicht enterprise. Don't overdo patterns, decoupling und was weiß der geier.


